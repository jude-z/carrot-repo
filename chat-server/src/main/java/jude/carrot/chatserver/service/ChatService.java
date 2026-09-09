package jude.carrot.chatserver.service;

import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.chatserver.key.snowflake.SnowFlakeKeyGenerator;
import jude.carrot.chatserver.response.ChatMessageResponse;
import jude.carrot.chatserver.response.CreateChatRoomResponse;
import jude.carrot.chatserver.response.FetchRecentChatMessage;
import jude.carrot.chatserver.response.PollingChatMessagesResponse;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.chat.ChatRepository;
import jude.carrot.infra.repository.chat.dto.CreateChatRoomRequest;
import jude.carrot.infra.repository.chat.dto.PublishChatRequest;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisChatRoomMessage;
import jude.carrot.infra.repository.chat.dto.RedisReadStatus;
import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.user.UserRepository;
import jude.carrot.service.exception.CustomException;
import jude.carrot.service.status.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;

import static jude.carrot.service.status.Status.USER_NOT_EXIST;

@RequiredArgsConstructor
@Service
public class ChatService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final RedisTemplate<String,Object> redisTemplate;
    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final ChatCacheService chatCacheService;
    private final ChatRetryService chatRetryService;
    private final SnowFlakeKeyGenerator snowFlakeKeyGenerator;

    public CreateChatRoomResponse createChatRoom(Long userId, CreateChatRoomRequest createChatRoomRequest) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(USER_NOT_EXIST));
        Long opponentId = createChatRoomRequest.opponentId();
        User opponentUser = userRepository.findById(opponentId)
                .orElseThrow(() -> new CustomException(USER_NOT_EXIST));
        ChatParticipant creatorChatParticipant = ChatParticipant.from(creator);
        ChatParticipant opponentChatParticipant = ChatParticipant.from(opponentUser);
        chatRepository.saveAll(creatorChatParticipant,opponentChatParticipant);
        String title = createChatRoomRequest.title();
        ChatRoom chatRoom = ChatRoom.from(title, creatorChatParticipant, opponentChatParticipant);
        chatRepository.save(chatRoom);
        Long chatRoomId = chatRoom.getId();
        return CreateChatRoomResponse.from(chatRoomId);
    }

    public FetchRecentChatMessage fetchRecentChatMessage(Long userId, Long chatRoomId) {

        chatRepository.fetchChatRoomByUserIdAndChatRoomId(userId, chatRoomId)
                .orElseThrow(() -> new CustomException(Status.CHAT_ROOM_NOT_EXIST));
        ChatMessage chatMessage = chatRepository.joinFetchChatMessage(chatRoomId)
                        .orElseThrow(() -> new CustomException(Status.CHAT_MESSAGE_NOT_EXIST));
        return FetchRecentChatMessage.from(chatMessage);
    }

    public ChatMessageResponse fetch(Integer pageNum, Integer pageSize, Long chatRoomId, Long userId){
        chatRepository.fetchChatRoomByUserIdAndChatRoomId(userId, chatRoomId)
                .orElseThrow(() -> new CustomException(Status.CHAT_ROOM_NOT_EXIST));
        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(pageNum-1,pageSize, sort);
        Page<ChatMessageElement> page = chatRepository.fetch(pageable,chatRoomId);
        return ChatMessageResponse.from(page);
    }
    public void publish(Long chatRoomId, Long userId, PublishChatRequest publishChatRequest) {
        saveChatMessage(chatRoomId, userId, publishChatRequest);
    }

    public ChatMessageElement publishForWebSocket(Long chatRoomId, Long userId, PublishChatRequest publishChatRequest) {
        RedisChatMessage redisChatMessage = saveChatMessage(chatRoomId, userId, publishChatRequest);
        return ChatMessageElement.from(redisChatMessage);
    }

    private RedisChatMessage saveChatMessage(Long chatRoomId, Long userId, PublishChatRequest publishChatRequest) {
        ChatParticipant chatParticipant = validateParticipant(chatRoomId, userId);
        Long chatParticipantId = chatParticipant.getId();
        String snowflakeId = snowFlakeKeyGenerator.generateSnowFlakeKey(LocalDateTime.now());
        RedisChatMessage redisChatMessage = RedisChatMessage.from(snowflakeId, publishChatRequest, LocalDateTime.now(), chatParticipantId);
        chatRetryService.saveRedis(chatRoomId, redisChatMessage);
        return redisChatMessage;
    }

    public Mono<PollingChatMessagesResponse> pollingFetch(Long chatRoomId, Long userId,String lastChatMessageId) {
        validateParticipant(chatRoomId, userId);
        String chatRoomMessageKey = ChatKeyGenerator.generateChatRoomMessageKey(chatRoomId);
        Range.Bound<Double> lowerBound = Range.Bound.exclusive(Double.valueOf(lastChatMessageId));

        return Flux.interval(Duration.ZERO, Duration.ofMillis(500))
                .take(Duration.ofMillis(4500))
                .concatMap(tick -> {
                    String currentSnowFlakeKey = snowFlakeKeyGenerator.generateSnowFlakeKey(LocalDateTime.now());
                    Range<Double> range = Range.of(lowerBound, Range.Bound.inclusive(Double.valueOf(currentSnowFlakeKey)));
                    return reactiveRedisTemplate.opsForZSet()
                        .rangeByScore(chatRoomMessageKey, range)
                        .map(RedisChatRoomMessage::chatMessageIdOf)
                        .map(ChatKeyGenerator::generateChatMessageKey)
                        .collectList();
                })
                .filter(list -> !list.isEmpty())
                .next()
                .flatMap(chatMessageKeys -> reactiveRedisTemplate.opsForValue()
                        .multiGet(chatMessageKeys))
                .publishOn(Schedulers.parallel())
                .map(list -> list.stream()
                        .map(RedisChatMessage::of)
                        .toList())
                .map(PollingChatMessagesResponse::from);
    }

    public void read(Long chatRoomId, Long userId, String chatMessageKey) {
        ChatParticipant chatParticipant = validateParticipant(chatRoomId, userId);
        String chatMessageId = ChatKeyGenerator.parseChatMessageId(chatMessageKey);
        if (chatMessageId == null) {
            throw new CustomException(Status.CHAT_MESSAGE_NOT_EXIST);
        }
        chatCacheService.fetchChatMessage(chatMessageId)
                .orElseThrow(() -> new CustomException(Status.CHAT_MESSAGE_NOT_EXIST));
        String readStatusKey = ChatKeyGenerator.generateReadStatusKey(chatParticipant.getId(), chatRoomId);
        redisTemplate.opsForValue().set(readStatusKey, RedisReadStatus.from(chatMessageId));
    }

    private ChatParticipant validateParticipant(Long chatRoomId, Long userId) {
        chatCacheService.fetchChatRoom(chatRoomId)
                .orElseThrow(() -> new CustomException(Status.CHAT_ROOM_NOT_EXIST));
        return chatCacheService.fetchChatParticipant(chatRoomId, userId)
                .orElseThrow(() -> new CustomException(Status.CHAT_PARTICIPANT_NOT_EXIST));
    }
}
