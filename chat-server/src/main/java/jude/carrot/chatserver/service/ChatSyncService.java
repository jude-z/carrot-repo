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
import java.util.stream.IntStream;

import static jude.carrot.chatserver.key.redis.ChatKeyGenerator.readStatusKeyPattern;

@Service
@RequiredArgsConstructor
public class ChatSyncService {
    private final ChatRepository chatRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public void syncChatMessage(){
        String chatMessagePattern = ChatKeyGenerator.chatMessageKeyPattern();
        ScanOptions scanOptions = from(chatMessagePattern, DataType.STRING);

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            List<String> keys = new ArrayList<>();
            while (cursor.hasNext()) {
                String findKey = cursor.next();
                keys.add(findKey);
                if (keys.size() == 1000) {
                    syncChatMessage(keys);
                    delete(keys);
                    keys.clear();
                }
            }
            if (!keys.isEmpty()) {
                syncChatMessage(keys);
                delete(keys);
            }
        }
    }

    public void syncChatRoomMessage(){
        String chatRoomMessagePattern = ChatKeyGenerator.chatRoomMessageKeyPattern();
        ScanOptions scanOptions = from(chatRoomMessagePattern, DataType.ZSET);

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            List<String> keys = new ArrayList<>();
            while (cursor.hasNext()) {
                String findKey = cursor.next();
                keys.add(findKey);
                if(keys.size() == 1000){
                    syncChatRoomMessage(keys);
                    delete(keys);
                    keys.clear();
                }
            }
            if(!keys.isEmpty()){
                syncChatRoomMessage(keys);
                delete(keys);
            }
        }


    }

    public void syncReadStatus(){
        String readStatusPattern = readStatusKeyPattern();
        ScanOptions scanOptions = from(readStatusPattern, DataType.STRING);

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            List<String> keys = new ArrayList<>();
            while (cursor.hasNext()) {
                String findKey = cursor.next();
                keys.add(findKey);
                if(keys.size() == 1000){
                    syncReadStatus(keys);
                    delete(keys);
                    keys.clear();
                }
            }
            if(!keys.isEmpty()){
                syncReadStatus(keys);
                delete(keys);
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
                            String key = keys.get(i);
                            String id = key.split("::")[1];
                            return ChatMessageBulk.from(id, redisChatMessages.get(i));
                        })
                        .toList();
        chatRepository.bulkChatMessage(chatMessageBulks);
    }
    private void syncChatRoomMessage(List<String> keys){
        List<ChatRoomMessageBulk> chatRoomMessageBulks = new ArrayList<>();
        for (String key : keys) {
            Long chatRoomId = Long.valueOf(key.split("::")[1]);
            Set<Object> members = redisTemplate.opsForZSet().range(key, 0, -1);
            for (Object member : members) {
                String chatMessageId = RedisChatRoomMessage.chatMessageIdOf(member);
                chatRoomMessageBulks.add(ChatRoomMessageBulk.from(chatRoomId, chatMessageId));
            }
            if(members.size() > 200) redisTemplate.opsForZSet().removeRange(key, 0, -201);
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
                    String key = keys.get(i);
                    Long chatParticipantId = Long.valueOf(key.split("::")[1]);
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
                .count(20L)
                .build();
    }
    private void delete(List<String> keys){
        redisTemplate.delete(keys);
    }

    public void reconciliation() {
        Set<String> liveSnowflakeIds = collectZSetMembers();
        String chatMessageKeyPattern = ChatKeyGenerator.chatMessageKeyPattern();
        ScanOptions scanOptions = from(chatMessageKeyPattern, DataType.STRING);

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            List<String> orphans = new ArrayList<>();
            while (cursor.hasNext()) {
                String key = cursor.next();
                String snowflakeId = ChatKeyGenerator.parseChatMessageId(key);
                if (!liveSnowflakeIds.contains(snowflakeId)) {
                    orphans.add(key);
                }
                if (orphans.size() == 1000) {
                    delete(orphans);
                    orphans.clear();
                }
            }
            if (!orphans.isEmpty()) {
                delete(orphans);
            }
        }
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
