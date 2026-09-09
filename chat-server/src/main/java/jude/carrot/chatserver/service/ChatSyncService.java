package jude.carrot.chatserver.service;

import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.infra.repository.chat.ChatRepository;
import jude.carrot.infra.repository.chat.dto.ChatMessageBulk;
import jude.carrot.infra.repository.chat.dto.ChatRoomMessageBulk;
import jude.carrot.infra.repository.chat.dto.ReadStatusBulk;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisChatRoomMessage;
import jude.carrot.infra.repository.chat.dto.RedisReadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static jude.carrot.chatserver.key.redis.ChatKeyGenerator.readStatusKeyPattern;

@Service
@RequiredArgsConstructor
public class ChatSyncService {
    private static final int BATCH_SIZE = 1000;
    private static final long SCAN_COUNT = 20L;
    private static final long ZSET_RETAIN_SIZE = 200L;

    private final ChatRepository chatRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public void syncChatMessage(){
        syncInBatches(ChatKeyGenerator.chatMessageKeyPattern(), DataType.STRING, this::syncChatMessage);
    }

    public void syncChatRoomMessage(){
        syncInBatches(ChatKeyGenerator.chatRoomMessageKeyPattern(), DataType.ZSET, this::syncChatRoomMessage);
    }

    public void syncReadStatus(){
        syncInBatches(readStatusKeyPattern(), DataType.STRING, this::syncReadStatus);
    }

    public void reconciliation() {
        Set<String> liveSnowflakeIds = collectZSetMembers();
        scanInBatches(ChatKeyGenerator.chatMessageKeyPattern(), DataType.STRING,
                key -> !liveSnowflakeIds.contains(ChatKeyGenerator.parseChatMessageId(key)),
                this::delete);
    }

    private void syncInBatches(String keyPattern, DataType dataType, Consumer<List<String>> sync) {
        scanInBatches(keyPattern, dataType, key -> true, keys -> {
            sync.accept(keys);
            delete(keys);
        });
    }

    private void scanInBatches(String keyPattern, DataType dataType,
                               Predicate<String> filter, Consumer<List<String>> batchHandler) {
        ScanOptions scanOptions = from(keyPattern, dataType);
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            List<String> keys = new ArrayList<>();
            while (cursor.hasNext()) {
                String key = cursor.next();
                if (!filter.test(key)) {
                    continue;
                }
                keys.add(key);
                if (keys.size() == BATCH_SIZE) {
                    batchHandler.accept(keys);
                    keys.clear();
                }
            }
            if (!keys.isEmpty()) {
                batchHandler.accept(keys);
            }
        }
    }

    private void syncChatMessage(List<String> keys){
        List<RedisChatMessage> redisChatMessages= redisTemplate.opsForValue().multiGet(keys)
                .stream()
                .map(RedisChatMessage::of)
                .toList();

        List<ChatMessageBulk> chatMessageBulks = IntStream.range(0,keys.size())
                        .mapToObj(i -> {
                            String id = ChatKeyGenerator.parseChatMessageId(keys.get(i));
                            return ChatMessageBulk.from(id, redisChatMessages.get(i));
                        })
                        .toList();
        chatRepository.bulkChatMessage(chatMessageBulks);
    }
    private void syncChatRoomMessage(List<String> keys){
        List<ChatRoomMessageBulk> chatRoomMessageBulks = new ArrayList<>();
        for (String key : keys) {
            Long chatRoomId = ChatKeyGenerator.parseChatRoomId(key);
            Set<Object> members = redisTemplate.opsForZSet().range(key, 0, -1);
            for (Object member : members) {
                String chatMessageId = RedisChatRoomMessage.chatMessageIdOf(member);
                chatRoomMessageBulks.add(ChatRoomMessageBulk.from(chatRoomId, chatMessageId));
            }
            if(members.size() > ZSET_RETAIN_SIZE) redisTemplate.opsForZSet().removeRange(key, 0, -(ZSET_RETAIN_SIZE + 1));
        }
        chatRepository.bulkChatRoomMessage(chatRoomMessageBulks);
    }
    private void syncReadStatus(List<String> keys){
        List<String> chatMessageIds= redisTemplate.opsForValue().multiGet(keys)
                .stream()
                .map(RedisReadStatus::chatMessageIdOf)
                .toList();
        List<ReadStatusBulk> readStatusBulks = IntStream.range(0, keys.size())
                .mapToObj(i -> {
                    Long chatParticipantId = ChatKeyGenerator.parseReadStatusChatParticipantId(keys.get(i));
                    String chatMessageId = chatMessageIds.get(i);
                    return ReadStatusBulk.from(chatParticipantId, chatMessageId);
                })
                .toList();
        chatRepository.bulkReadStatus(readStatusBulks);
    }

    private ScanOptions from(String keyPattern, DataType dataType){
        return ScanOptions.scanOptions()
                .match(keyPattern)
                .type(dataType)
                .count(SCAN_COUNT)
                .build();
    }
    private void delete(List<String> keys){
        redisTemplate.delete(keys);
    }

    private Set<String> collectZSetMembers() {
        Set<String> snowflakeIds = new HashSet<>();
        String chatRoomMessagePattern = ChatKeyGenerator.chatRoomMessageKeyPattern();
        ScanOptions scanOptions = from(chatRoomMessagePattern, DataType.ZSET);

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String zsetKey = cursor.next();
                ScanOptions memberScan = ScanOptions.scanOptions().count(1000L).build();
                try (Cursor<ZSetOperations.TypedTuple<Object>> zcursor =
                             redisTemplate.opsForZSet().scan(zsetKey, memberScan)) {
                    while (zcursor.hasNext()) {
                        snowflakeIds.add(RedisChatRoomMessage.chatMessageIdOf(zcursor.next().getValue()));
                    }
                }
            }
        }
        return snowflakeIds;
    }

}
