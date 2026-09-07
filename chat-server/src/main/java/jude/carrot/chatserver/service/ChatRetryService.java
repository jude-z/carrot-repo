package jude.carrot.chatserver.service;

import io.lettuce.core.RedisException;
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
    public void saveRedis(String chatMessageKey, String chatRoomMessageKey, RedisChatMessage redisChatMessage, RedisChatRoomMessage redisChatRoomMessage, Double score){
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
    public void recover(RedisException e){
        log.error("error",e);
        throw new CustomException(Status.PUBLISH_CHAT_MESSAGE_FAIL);
    }
}
