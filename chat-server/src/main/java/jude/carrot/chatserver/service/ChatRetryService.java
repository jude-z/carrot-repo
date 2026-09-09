package jude.carrot.chatserver.service;

import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisChatRoomMessage;
import jude.carrot.service.exception.CustomException;
import jude.carrot.service.status.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRetryService {

    private final RedisTemplate<String,Object> redisTemplate;

    @Retryable
    public void saveRedis(Long chatRoomId, RedisChatMessage redisChatMessage){
        String snowflakeId = redisChatMessage.id();
        String chatMessageKey = ChatKeyGenerator.generateChatMessageKey(snowflakeId);
        String chatRoomMessageKey = ChatKeyGenerator.generateChatRoomMessageKey(chatRoomId);
        RedisChatRoomMessage redisChatRoomMessage = RedisChatRoomMessage.from(snowflakeId);
        double score = Double.parseDouble(snowflakeId);
        executeInTransaction(ops -> {
            ops.opsForValue().set(chatMessageKey, redisChatMessage);
            ops.opsForZSet().add(chatRoomMessageKey, redisChatRoomMessage, score);
        });
    }

    private List<Object> executeInTransaction(Consumer<RedisOperations<String, Object>> body) {
        return redisTemplate.execute(new SessionCallback<>() {
            @Override
            public <K, V> List<Object> execute(RedisOperations<K, V> operations) {
                RedisOperations<String, Object> ops = (RedisOperations<String, Object>) operations;
                ops.multi();
                body.accept(ops);
                return ops.exec();
            }
        });
    }

    @Recover
    public void recover(Exception e, Long chatRoomId, RedisChatMessage redisChatMessage){
        log.error("failed to save chat message {} of chatRoom {}", redisChatMessage.id(), chatRoomId, e);
        throw new CustomException(Status.PUBLISH_CHAT_MESSAGE_FAIL);
    }
}
