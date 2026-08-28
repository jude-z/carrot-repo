package jude.carrot.chatserver.service;

import io.lettuce.core.RedisException;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisChatRoomMessage;
import jude.carrot.service.exception.CustomException;
import jude.carrot.service.status.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRetryService {

    private final RedisTemplate<String,Object> redisTemplate;

    @Retryable
    public void saveRedis(String chatMessageKey, String chatRoomMessageKey, RedisChatMessage redisChatMessage, RedisChatRoomMessage redisChatRoomMessage, Double score){
        redisTemplate.opsForValue().set(chatMessageKey, redisChatMessage);
        redisTemplate.opsForZSet().add(chatRoomMessageKey, redisChatRoomMessage, score);
    }

    @Recover
    public void recover(RedisException e){
        log.error("error",e);
        throw new CustomException(Status.PUBLISH_CHAT_MESSAGE_FAIL);
    }
}
