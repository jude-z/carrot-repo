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
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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
        chatCacheService.fetchChatRoom(chatRoomId)
                .orElseThrow(() -> new CustomException(Status.CHAT_ROOM_NOT_EXIST));
        ChatParticipant chatParticipant = chatCacheService.fetchChatParticipant(chatRoomId, userId)
                .orElseThrow(() -> new CustomException(Status.CHAT_PARTICIPANT_NOT_EXIST));
        Long chatParticipantId = chatParticipant.getId();
        String snowflakeId = snowFlakeKeyGenerator.generateSnowFlakeKey(LocalDateTime.now());
        RedisChatMessage redisChatMessage = RedisChatMessage.from(snowflakeId, publishChatRequest, LocalDateTime.now(),chatParticipantId);
        RedisChatRoomMessage redisChatRoomMessage = RedisChatRoomMessage.from(snowflakeId);
        String chatMessageKey = ChatKeyGenerator.generateChatMessageKey(snowflakeId);
        String chatRoomMessageKey = ChatKeyGenerator.generateChatRoomMessageKey(chatRoomId);
        double score = Double.parseDouble(snowflakeId);
        chatRetryService.saveRedis(chatMessageKey, chatRoomMessageKey, redisChatMessage, redisChatRoomMessage, score);
    }

    public Mono<PollingChatMessagesResponse> pollingFetch(Long chatRoomId, Long userId,String lastChatMessageId) {
        chatCacheService.fetchChatRoom(chatRoomId)
                .orElseThrow(() -> new CustomException(Status.CHAT_ROOM_NOT_EXIST));
        chatCacheService.fetchChatParticipant(chatRoomId, userId)
                .orElseThrow(() -> new CustomException(Status.CHAT_PARTICIPANT_NOT_EXIST));
        String chatRoomMessageKey = ChatKeyGenerator.generateChatRoomMessageKey(chatRoomId);
        // 클라이언트가 이미 받은 마지막 메시지는 제외(배타). 상한은 폴링 도중 발행된 메시지를 놓치지 않도록 틱마다 다시 계산한다.
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
        chatCacheService.fetchChatRoom(chatRoomId)
                .orElseThrow(() -> new CustomException(Status.CHAT_ROOM_NOT_EXIST));
        chatCacheService.fetchChatParticipant(chatRoomId, userId)
                .orElseThrow(() -> new CustomException(Status.CHAT_PARTICIPANT_NOT_EXIST));
        chatCacheService.fetchChatMessage(chatMessageKey)
                .orElseThrow(() -> new CustomException(Status.CHAT_MESSAGE_NOT_EXIST));
        String readStatusKey = ChatKeyGenerator.generateReadStatusKey(userId, chatRoomId);
        String chatMessageId = ChatKeyGenerator.parseChatMessageId(chatMessageKey);
        redisTemplate.opsForValue().set(readStatusKey, RedisReadStatus.from(chatMessageId));
    }
}
