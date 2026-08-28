package jude.carrot.chatserver.service;

import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.infra.repository.chat.ChatRepository;
import jude.carrot.infra.repository.chat.dto.ChatMessageBulk;
import jude.carrot.infra.repository.chat.dto.ChatRoomMessageBulk;
import jude.carrot.infra.repository.chat.dto.ReadStatusBulk;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisChatRoomMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
                    keys.clear();
                }
            }
            if (!keys.isEmpty()) {
                syncChatMessage(keys);
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
                    keys.clear();
                }
            }
            if(!keys.isEmpty()){
                syncChatRoomMessage(keys);
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
                    keys.clear();
                }
            }
            if(!keys.isEmpty()){
                syncReadStatus(keys);
            }
        }
    }

    private void syncChatMessage(List<String> keys){
        List<RedisChatMessage> redisChatMessages= redisTemplate.opsForValue().multiGet(keys)
                .stream()
                .map(chatMessage -> (RedisChatMessage) chatMessage)
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
        List<RedisChatRoomMessage> redisChatMessages= redisTemplate.opsForValue().multiGet(keys)
                .stream()
                .map(chatMessage -> (RedisChatRoomMessage) chatMessage)
                .toList();

        List<ChatRoomMessageBulk> chatRoomMessageBulks = IntStream.range(0,keys.size())
                .mapToObj(i -> {
                    String key = keys.get(i);
                    Long chatRoomId = Long.valueOf(key.split("::")[1]);
                    return ChatRoomMessageBulk.from(chatRoomId, redisChatMessages.get(i));
                })
                .toList();
        chatRepository.bulkChatRoomMessage(chatRoomMessageBulks);
    }
    private void syncReadStatus(List<String> keys){
        List<String> chatMessageIds= redisTemplate.opsForValue().multiGet(keys)
                .stream()
                .map(String::valueOf)
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

}
