package jude.carrot.infra.repository.chat;

import jude.carrot.infra.InfraTestConfig;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.entity.chat.ReadStatus;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.chat.dto.ChatMessageBulk;
import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.chat.dto.ChatRoomMessageBulk;
import jude.carrot.infra.repository.chat.dto.ReadStatusBulk;
import jude.carrot.infra.repository.chat.jpa.ChatMessageJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatParticipantJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatRoomJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ReadStatusJpaRepository;
import jude.carrot.infra.repository.user.jpa.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static jude.carrot.infra.repository.chat.ChatRepositoryTest.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = InfraTestConfig.class)
@Testcontainers
@Transactional
@Import(ChatRepositoryConfig.class)
class ChatRepositoryTest {

    @TestConfiguration
    static class ChatRepositoryConfig {
        @Bean
        ChatRepository chatRepository(ChatMessageJpaRepository chatMessageJpaRepository,
                                      ChatParticipantJpaRepository chatParticipantJpaRepository,
                                      ChatRoomJpaRepository chatRoomJpaRepository,
                                      ReadStatusJpaRepository readStatusJpaRepository,
                                      DataSource dataSource) {
            return new ChatRepositoryImpl(chatMessageJpaRepository, chatParticipantJpaRepository,
                    chatRoomJpaRepository, readStatusJpaRepository, dataSource);
        }
    }

    static final String DATABASE_NAME = "test";
    static final String DATABASE_USERNAME = "test";
    static final String DATABASE_PASSWORD = UUID.randomUUID().toString();
    static final AtomicLong MESSAGE_ID_SEQUENCE = new AtomicLong(System.currentTimeMillis());

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD);

    @Autowired
    ChatRepository chatRepository;
    @Autowired
    UserJpaRepository userJpaRepository;
    @Autowired
    ReadStatusJpaRepository readStatusJpaRepository;
    @Autowired
    TestEntityManager entityManager;

    User creatorUser;
    User opponentUser;
    ChatParticipant creator;
    ChatParticipant opponent;

    @BeforeEach
    void setUp() {
        creatorUser = userJpaRepository.save(User.builder().email("creator@carrot.com").password("password").build());
        opponentUser = userJpaRepository.save(User.builder().email("opponent@carrot.com").password("password").build());
        creator = ChatParticipant.from(creatorUser);
        opponent = ChatParticipant.from(opponentUser);
        chatRepository.saveAll(creator, opponent);
    }

    @Test
    @DisplayName("채팅 참여자를 여러 명 저장하면 각각 id가 채번된다")
    void saveAllAssignsIdToEachParticipant() {
        assertThat(creator.getId()).isNotNull();
        assertThat(opponent.getId()).isNotNull();
    }

    @Test
    @DisplayName("채팅방을 저장하면 id가 채번된다")
    void saveChatRoomAssignsId() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);

        chatRepository.save(chatRoom);

        assertThat(chatRoom.getId()).isNotNull();
    }

    @Test
    @DisplayName("채팅 메시지를 저장하면 애플리케이션이 부여한 id로 조회된다")
    void saveChatMessageSuccess() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("hello").chatRoom(chatRoom).publishedBy(creator).build();
        String messageId = chatMessage.getId();

        chatRepository.save(chatMessage);
        entityManager.flush();
        entityManager.clear();

        ChatMessage found = entityManager.find(ChatMessage.class, messageId);
        assertThat(found).isNotNull();
        assertThat(found.getContent()).isEqualTo("hello");
    }

    @Test
    @DisplayName("읽음 상태를 저장하면 id가 채번된다")
    void saveReadStatusAssignsId() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("hello").chatRoom(chatRoom).publishedBy(creator).build();
        chatRepository.save(chatMessage);
        ReadStatus readStatus = ReadStatus.from(opponent, chatMessage);

        chatRepository.save(readStatus);

        assertThat(readStatus.getId()).isNotNull();
    }

    @Test
    @DisplayName("채팅방 생성자가 조회하면 채팅방이 반환된다")
    void fetchChatRoomByUserIdAndChatRoomIdSuccessWhenCreator() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatRoom> found = chatRepository.fetchChatRoomByUserIdAndChatRoomId(creatorUser.getId(), chatRoom.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(c -> assertThat(c.getTitle()).isEqualTo("title"));
    }

    @Test
    @DisplayName("채팅방 상대방이 조회해도 채팅방이 반환된다")
    void fetchChatRoomByUserIdAndChatRoomIdSuccessWhenOpponent() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatRoom> found = chatRepository.fetchChatRoomByUserIdAndChatRoomId(opponentUser.getId(), chatRoom.getId());

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("채팅방과 무관한 회원이 조회하면 비어있다")
    void fetchChatRoomByUserIdAndChatRoomIdFailWhenNotParticipant() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        User strangerUser = userJpaRepository.save(User.builder().email("stranger@carrot.com").password("password").build());
        entityManager.flush();
        entityManager.clear();

        Optional<ChatRoom> found = chatRepository.fetchChatRoomByUserIdAndChatRoomId(strangerUser.getId(), chatRoom.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("채팅방의 메시지를 join fetch로 조회하면 채팅방과 작성자가 함께 조회된다")
    void joinFetchChatMessageSuccess() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("hello").chatRoom(chatRoom).publishedBy(creator).build();
        chatRepository.save(chatMessage);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatMessage> found = chatRepository.joinFetchChatMessage(chatRoom.getId());

        assertThat(found).isPresent();
        ChatMessage fetched = found.get();
        assertThat(fetched.getContent()).isEqualTo("hello");
        assertThat(fetched.getChatRoom().getId()).isEqualTo(chatRoom.getId());
        assertThat(fetched.getPublishedBy().getId()).isEqualTo(creator.getId());
    }

    @Test
    @DisplayName("채팅방에 메시지가 여러 건이면 가장 최근에 저장된 메시지만 조회된다")
    void joinFetchChatMessageSuccessReturnsMostRecentOnly() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage firstMessage = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("first").chatRoom(chatRoom).publishedBy(creator).build();
        chatRepository.save(firstMessage);
        ChatMessage secondMessage = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("second").chatRoom(chatRoom).publishedBy(opponent).build();
        chatRepository.save(secondMessage);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatMessage> found = chatRepository.joinFetchChatMessage(chatRoom.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(c -> assertThat(c.getContent()).isEqualTo("second"));
    }

    @Test
    @DisplayName("메시지가 없는 채팅방을 조회하면 비어있다")
    void joinFetchChatMessageFailWhenNotExist() {
        Optional<ChatMessage> found = chatRepository.joinFetchChatMessage(-1L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("메시지를 벌크로 insert하면 채팅방 없이도 저장된다")
    void bulkChatMessageInsertsMessageDirectly() {
        String messageId = UUID.randomUUID().toString();
        ChatMessageBulk bulk = ChatMessageBulk.builder()
                .id(messageId)
                .content("bulk message")
                .publishedById(creator.getId())
                .publishedAt(LocalDateTime.now())
                .build();

        chatRepository.bulkChatMessage(List.of(bulk));
        entityManager.clear();

        ChatMessage found = entityManager.find(ChatMessage.class, messageId);
        assertThat(found).isNotNull();
        assertThat(found.getContent()).isEqualTo("bulk message");
        assertThat(found.getPublishedBy().getId()).isEqualTo(creator.getId());
    }

    @Test
    @DisplayName("메시지를 벌크로 채팅방에 연결하면 채팅방이 설정된다")
    void bulkChatRoomMessageLinksMessageToChatRoom() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        String messageId = UUID.randomUUID().toString();
        chatRepository.bulkChatMessage(List.of(ChatMessageBulk.builder()
                .id(messageId)
                .content("hello")
                .publishedById(creator.getId())
                .publishedAt(LocalDateTime.now())
                .build()));

        chatRepository.bulkChatRoomMessage(List.of(ChatRoomMessageBulk.builder()
                .chatRoomId(chatRoom.getId())
                .chatMessageId(messageId)
                .build()));
        entityManager.clear();

        ChatMessage found = entityManager.find(ChatMessage.class, messageId);
        assertThat(found).isNotNull();
        assertThat(found.getChatRoom().getId()).isEqualTo(chatRoom.getId());
    }

    @Test
    @DisplayName("읽음 상태를 벌크로 저장하면 읽음 상태가 채번된다")
    void bulkReadStatusInsertsReadStatus() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("hello").chatRoom(chatRoom).publishedBy(creator).build();
        chatRepository.save(chatMessage);
        entityManager.flush();
        LocalDateTime now = LocalDateTime.now();

        chatRepository.bulkReadStatus(List.of(ReadStatusBulk.builder()
                .chatParticipantId(opponent.getId())
                .chatMessageId(chatMessage.getId())
                .createTime(now)
                .updateTime(now)
                .build()));
        entityManager.clear();

        List<ReadStatus> readStatuses = readStatusJpaRepository.findAll();
        assertThat(readStatuses).hasSize(1);
        assertThat(readStatuses.get(0).getChatMessage().getId()).isEqualTo(chatMessage.getId());
        assertThat(readStatuses.get(0).getChatParticipant().getId()).isEqualTo(opponent.getId());
    }

    @Test
    @DisplayName("채팅방의 메시지를 최신순으로 페이지 단위 조회한다")
    void fetchReturnsMessagesForChatRoomOrderedByNewest() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage first = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("first").chatRoom(chatRoom).publishedBy(creator).build();
        chatRepository.save(first);
        ChatMessage second = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("second").chatRoom(chatRoom).publishedBy(opponent).build();
        chatRepository.save(second);
        entityManager.flush();
        entityManager.clear();

        Page<ChatMessageElement> page = chatRepository.fetch(PageRequest.of(0, 10), chatRoom.getId());

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ChatMessageElement::content)
                .containsExactly("second", "first");
    }

    @Test
    @DisplayName("메시지가 없는 채팅방을 페이지 조회하면 빈 페이지가 반환된다")
    void fetchReturnsEmptyPageWhenNoMessages() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Page<ChatMessageElement> page = chatRepository.fetch(PageRequest.of(0, 10), chatRoom.getId());

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("id로 채팅방을 조회하면 반환된다")
    void fetchChatRoomSuccess() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatRoom> found = chatRepository.fetchChatRoom(chatRoom.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(c -> assertThat(c.getTitle()).isEqualTo("title"));
    }

    @Test
    @DisplayName("존재하지 않는 id로 채팅방을 조회하면 비어있다")
    void fetchChatRoomFailWhenNotExist() {
        Optional<ChatRoom> found = chatRepository.fetchChatRoom(-1L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("생성자가 채팅 참여자를 조회하면 본인의 참여자 정보가 반환된다")
    void fetchChatParticipantSuccessWhenCreator() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatParticipant> found = chatRepository.fetchChatParticipant(chatRoom.getId(), creatorUser.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(p -> assertThat(p.getId()).isEqualTo(creator.getId()));
    }

    @Test
    @DisplayName("상대방이 채팅 참여자를 조회하면 본인의 참여자 정보가 반환된다")
    void fetchChatParticipantSuccessWhenOpponent() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatParticipant> found = chatRepository.fetchChatParticipant(chatRoom.getId(), opponentUser.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(p -> assertThat(p.getId()).isEqualTo(opponent.getId()));
    }

    @Test
    @DisplayName("채팅방과 무관한 회원이 참여자를 조회하면 비어있다")
    void fetchChatParticipantFailWhenNotParticipant() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        User strangerUser = userJpaRepository.save(User.builder().email("stranger2@carrot.com").password("password").build());
        entityManager.flush();
        entityManager.clear();

        Optional<ChatParticipant> found = chatRepository.fetchChatParticipant(chatRoom.getId(), strangerUser.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("id로 채팅 메시지를 조회하면 반환된다")
    void fetchChatMessageSuccess() {
        ChatRoom chatRoom = ChatRoom.from("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatMessage.builder().id(String.valueOf(MESSAGE_ID_SEQUENCE.incrementAndGet())).content("hello").chatRoom(chatRoom).publishedBy(creator).build();
        chatRepository.save(chatMessage);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatMessage> found = chatRepository.fetchChatMessage(chatMessage.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(m -> assertThat(m.getContent()).isEqualTo("hello"));
    }

    @Test
    @DisplayName("존재하지 않는 id로 채팅 메시지를 조회하면 비어있다")
    void fetchChatMessageFailWhenNotExist() {
        Optional<ChatMessage> found = chatRepository.fetchChatMessage("not-exist");

        assertThat(found).isEmpty();
    }
}
