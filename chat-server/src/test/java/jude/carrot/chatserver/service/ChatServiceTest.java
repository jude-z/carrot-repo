package jude.carrot.chatserver.service;

import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.chatserver.key.snowflake.SnowFlakeKeyGenerator;
import jude.carrot.chatserver.response.ChatMessageResponse;
import jude.carrot.chatserver.response.FetchRecentChatMessage;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.chat.ChatRepository;
import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.chat.dto.CreateChatRoomRequest;
import jude.carrot.infra.repository.chat.dto.PublishChatRequest;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisReadStatus;
import jude.carrot.infra.repository.user.UserRepository;
import jude.carrot.service.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static jude.carrot.service.status.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long OPPONENT_ID = 2L;
    private static final Long CHAT_ROOM_ID = 10L;
    private static final Long CHAT_PARTICIPANT_ID = 100L;

    @InjectMocks
    private ChatService chatService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ChatRepository chatRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    @Mock
    private ChatCacheService chatCacheService;
    @Mock
    private SnowFlakeKeyGenerator snowFlakeKeyGenerator;
    @Mock
    private ChatRetryService chatRetryService;

    private User creator;
    private User opponent;
    private ChatParticipant chatParticipant;

    @BeforeEach
    void setUp() {
        creator = User.from("creator@carrot.com", "encoded", "creator", null);
        creator.setId(USER_ID);
        opponent = User.from("opponent@carrot.com", "encoded", "opponent", null);
        opponent.setId(OPPONENT_ID);
        chatParticipant = ChatParticipant.from(creator);
        chatParticipant.setId(CHAT_PARTICIPANT_ID);
    }


    @Test
    @DisplayName("생성자가 존재하지 않으면 CustomException(USER_NOT_EXIST)을 던진다")
    void createChatRoom_fail_whenCreatorNotExist() {
        CreateChatRoomRequest request = CreateChatRoomRequest.builder().opponentId(OPPONENT_ID).title("title").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createChatRoom(USER_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());

        verify(chatRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("상대방이 존재하지 않으면 CustomException(USER_NOT_EXIST)을 던진다")
    void createChatRoom_fail_whenOpponentNotExist() {
        CreateChatRoomRequest request = CreateChatRoomRequest.builder().opponentId(OPPONENT_ID).title("title").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(creator));
        when(userRepository.findById(OPPONENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.createChatRoom(USER_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());

        verify(chatRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("생성자와 상대방이 모두 존재하면 참여자와 채팅방을 저장한다")
    void createChatRoom_success() {
        CreateChatRoomRequest request = CreateChatRoomRequest.builder().opponentId(OPPONENT_ID).title("우리 동네").build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(creator));
        when(userRepository.findById(OPPONENT_ID)).thenReturn(Optional.of(opponent));

        chatService.createChatRoom(USER_ID, request);

        ArgumentCaptor<ChatParticipant> creatorCaptor = ArgumentCaptor.forClass(ChatParticipant.class);
        ArgumentCaptor<ChatParticipant> opponentCaptor = ArgumentCaptor.forClass(ChatParticipant.class);
        verify(chatRepository).saveAll(creatorCaptor.capture(), opponentCaptor.capture());
        assertThat(creatorCaptor.getValue().getUser()).isEqualTo(creator);
        assertThat(opponentCaptor.getValue().getUser()).isEqualTo(opponent);

        ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        verify(chatRepository).save(roomCaptor.capture());
        assertThat(roomCaptor.getValue().getTitle()).isEqualTo("우리 동네");
        assertThat(roomCaptor.getValue().getCreator().getUser()).isEqualTo(creator);
        assertThat(roomCaptor.getValue().getOpponent().getUser()).isEqualTo(opponent);
    }


    @Test
    @DisplayName("채팅방에 속하지 않은 사용자가 조회하면 CustomException(CHAT_ROOM_NOT_EXIST)을 던진다")
    void fetchRecentChatMessage_fail_whenChatRoomNotExist() {
        when(chatRepository.fetchChatRoomByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.fetchRecentChatMessage(USER_ID, CHAT_ROOM_ID))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_ROOM_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("채팅방에 메시지가 없으면 CustomException(CHAT_MESSAGE_NOT_EXIST)을 던진다")
    void fetchRecentChatMessage_fail_whenChatMessageNotExist() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        when(chatRepository.fetchChatRoomByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatRepository.joinFetchChatMessage(CHAT_ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.fetchRecentChatMessage(USER_ID, CHAT_ROOM_ID))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_MESSAGE_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("최근 채팅 메시지를 조회하면 발행자 정보를 포함한 응답을 반환한다")
    void fetchRecentChatMessage_success() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        ChatMessage chatMessage = ChatMessage.builder()
                .id("1").content("hello").publishedBy(chatParticipant).publishedAt(LocalDateTime.now()).build();
        when(chatRepository.fetchChatRoomByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatRepository.joinFetchChatMessage(CHAT_ROOM_ID)).thenReturn(Optional.of(chatMessage));

        FetchRecentChatMessage response = chatService.fetchRecentChatMessage(USER_ID, CHAT_ROOM_ID);

        assertThat(response.id()).isEqualTo("1");
        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.publishedById()).isEqualTo(USER_ID);
    }


    @Test
    @DisplayName("채팅방에 속하지 않은 사용자가 메시지 목록을 조회하면 CustomException(CHAT_ROOM_NOT_EXIST)을 던진다")
    void fetch_fail_whenChatRoomNotExist() {
        when(chatRepository.fetchChatRoomByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.fetch(1, 20, CHAT_ROOM_ID, USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_ROOM_NOT_EXIST.getHttpStatus());

        verify(chatRepository, never()).fetch(any(), any());
    }

    @Test
    @DisplayName("메시지 목록을 페이지 단위로 조회한다")
    void fetch_success() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        ChatMessageElement element = ChatMessageElement.builder()
                .id("1").content("hi").publishedBy(USER_ID).publishedAt(LocalDateTime.now()).build();
        Page<ChatMessageElement> page = new PageImpl<>(List.of(element), PageRequest.of(1, 20), 21);
        when(chatRepository.fetchChatRoomByUserIdAndChatRoomId(USER_ID, CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatRepository.fetch(any(Pageable.class), eq(CHAT_ROOM_ID))).thenReturn(page);

        ChatMessageResponse response = chatService.fetch(2, 20, CHAT_ROOM_ID, USER_ID);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(chatRepository).fetch(pageableCaptor.capture(), eq(CHAT_ROOM_ID));
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(response.page()).containsExactly(element);
    }

    // ---------- publish ----------

    @Test
    @DisplayName("존재하지 않는 채팅방에 발행하면 CustomException(CHAT_ROOM_NOT_EXIST)을 던진다")
    void publish_fail_whenChatRoomNotExist() {
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.empty());
        PublishChatRequest request = PublishChatRequest.builder().content("hi").build();

        assertThatThrownBy(() -> chatService.publish(CHAT_ROOM_ID, USER_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_ROOM_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 CustomException(CHAT_PARTICIPANT_NOT_EXIST)을 던진다")
    void publish_fail_whenChatParticipantNotExist() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatCacheService.fetchChatParticipant(CHAT_ROOM_ID, USER_ID)).thenReturn(Optional.empty());
        PublishChatRequest request = PublishChatRequest.builder().content("hi").build();

        assertThatThrownBy(() -> chatService.publish(CHAT_ROOM_ID, USER_ID, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_PARTICIPANT_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("메시지를 발행하면 채팅방 ID와 메시지를 ChatRetryService에 위임한다")
    void publish_success() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatCacheService.fetchChatParticipant(CHAT_ROOM_ID, USER_ID)).thenReturn(Optional.of(chatParticipant));
        when(snowFlakeKeyGenerator.generateSnowFlakeKey(any(LocalDateTime.class))).thenReturn("123456789");
        PublishChatRequest request = PublishChatRequest.builder().content("hello").build();

        chatService.publish(CHAT_ROOM_ID, USER_ID, request);

        ArgumentCaptor<RedisChatMessage> messageCaptor = ArgumentCaptor.forClass(RedisChatMessage.class);
        verify(chatRetryService).saveRedis(eq(CHAT_ROOM_ID), messageCaptor.capture());
        assertThat(messageCaptor.getValue().id()).isEqualTo("123456789");
        assertThat(messageCaptor.getValue().content()).isEqualTo("hello");
        assertThat(messageCaptor.getValue().publishedBy()).isEqualTo(CHAT_PARTICIPANT_ID);
    }

    // ---------- pollingFetch ----------

    @Test
    @DisplayName("존재하지 않는 채팅방을 폴링하면 CustomException(CHAT_ROOM_NOT_EXIST)을 던진다")
    void pollingFetch_fail_whenChatRoomNotExist() {
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.pollingFetch(CHAT_ROOM_ID, USER_ID, "1000"))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_ROOM_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 폴링 시 CustomException(CHAT_PARTICIPANT_NOT_EXIST)을 던진다")
    void pollingFetch_fail_whenChatParticipantNotExist() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatCacheService.fetchChatParticipant(CHAT_ROOM_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.pollingFetch(CHAT_ROOM_ID, USER_ID, "1000"))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_PARTICIPANT_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("새 메시지가 생기면 폴링 결과로 새 메시지 목록을 반환한다")
    void pollingFetch_success_whenNewMessageArrives() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatCacheService.fetchChatParticipant(CHAT_ROOM_ID, USER_ID)).thenReturn(Optional.of(chatParticipant));
        when(snowFlakeKeyGenerator.generateSnowFlakeKey(any(LocalDateTime.class))).thenReturn("2000");

        String chatRoomMessageKey = ChatKeyGenerator.generateChatRoomMessageKey(CHAT_ROOM_ID);
        String chatMessageKey = ChatKeyGenerator.generateChatMessageKey("2000");
        // string 값도 실제 Redis에서는 Map으로 역직렬화된다
        Map<String, Object> redisChatMessage = Map.of(
                "id", "2000", "content", "new message", "publishedBy", CHAT_PARTICIPANT_ID, "publishedAt", "2026-09-07T12:30:15");

        ReactiveZSetOperations<String, Object> reactiveZSetOperations = mock(ReactiveZSetOperations.class);
        ReactiveValueOperations<String, Object> reactiveValueOperations = mock(ReactiveValueOperations.class);
        when(reactiveRedisTemplate.opsForZSet()).thenReturn(reactiveZSetOperations);
        when(reactiveRedisTemplate.opsForValue()).thenReturn(reactiveValueOperations);
        // zset 멤버는 실제 Redis에서 Map으로 역직렬화되므로 그 형태 그대로 준다
        when(reactiveZSetOperations.rangeByScore(eq(chatRoomMessageKey), any(Range.class)))
                .thenReturn(Flux.just(Map.of("chatMessageId", "2000")));
        when(reactiveValueOperations.multiGet(List.of(chatMessageKey)))
                .thenReturn(Mono.just(List.of(redisChatMessage)));

        StepVerifier.withVirtualTime(() -> chatService.pollingFetch(CHAT_ROOM_ID, USER_ID, "1000"))
                .thenAwait(Duration.ofMillis(500))
                .assertNext(response -> {
                    assertThat(response.elementCount()).isEqualTo(1);
                    assertThat(response.elements())
                            .extracting("id", "content")
                            .containsExactly(org.assertj.core.groups.Tuple.tuple("2000", "new message"));
                })
                .verifyComplete();
    }


    @Test
    @DisplayName("존재하지 않는 채팅방을 읽음 처리하면 CustomException(CHAT_ROOM_NOT_EXIST)을 던진다")
    void read_fail_whenChatRoomNotExist() {
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.read(CHAT_ROOM_ID, USER_ID, "chatMessage::1"))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_ROOM_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("채팅방 참여자가 아니면 읽음 처리 시 CustomException(CHAT_PARTICIPANT_NOT_EXIST)을 던진다")
    void read_fail_whenChatParticipantNotExist() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatCacheService.fetchChatParticipant(CHAT_ROOM_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.read(CHAT_ROOM_ID, USER_ID, "chatMessage::1"))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_PARTICIPANT_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("존재하지 않는 채팅 메시지를 읽음 처리하면 CustomException(CHAT_MESSAGE_NOT_EXIST)을 던진다")
    void read_fail_whenChatMessageNotExist() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatCacheService.fetchChatParticipant(CHAT_ROOM_ID, USER_ID)).thenReturn(Optional.of(chatParticipant));
        when(chatCacheService.fetchChatMessage("chatMessage::1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.read(CHAT_ROOM_ID, USER_ID, "chatMessage::1"))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(CHAT_MESSAGE_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("읽음 처리를 요청하면 읽음 상태 키로 Redis에 저장한다")
    void read_success() {
        ChatRoom chatRoom = ChatRoom.from("title", chatParticipant, chatParticipant);
        ChatMessage chatMessage = ChatMessage.builder().id("1").content("hi").build();
        when(chatCacheService.fetchChatRoom(CHAT_ROOM_ID)).thenReturn(Optional.of(chatRoom));
        when(chatCacheService.fetchChatParticipant(CHAT_ROOM_ID, USER_ID)).thenReturn(Optional.of(chatParticipant));
        when(chatCacheService.fetchChatMessage("chatMessage::1")).thenReturn(Optional.of(chatMessage));
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        chatService.read(CHAT_ROOM_ID, USER_ID, "chatMessage::1");

        String expectedKey = ChatKeyGenerator.generateReadStatusKey(USER_ID, CHAT_ROOM_ID);
        ArgumentCaptor<RedisReadStatus> captor = ArgumentCaptor.forClass(RedisReadStatus.class);
        verify(valueOperations).set(eq(expectedKey), captor.capture());
        assertThat(captor.getValue().chatMessageId()).isEqualTo("1");
    }
}
