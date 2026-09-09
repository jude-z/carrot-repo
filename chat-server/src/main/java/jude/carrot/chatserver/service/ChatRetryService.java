package jude.carrot.chatserver.service;

import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisChatRoomMessage;
import jude.carrot.service.exception.CustomException;
import jude.carrot.service.status.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRetryService {

    static final RedisScript<Long> PUBLISH_SCRIPT = RedisScript.of("""
            redis.call('SET', KEYS[1], ARGV[1])
            redis.call('ZADD', KEYS[2], ARGV[2], ARGV[3])
            return 1
            """, Long.class);

    private static final RedisSerializer<String> ARGS_SERIALIZER = new StringRedisSerializer();
    private static final RedisSerializer<Long> RESULT_SERIALIZER = new GenericToStringSerializer<>(Long.class);

    private final RedisTemplate<String, Object> redisTemplate;

    @Retryable
    public void saveRedis(Long chatRoomId, RedisChatMessage redisChatMessage) {
        String snowflakeId = redisChatMessage.id();
        String chatMessageKey = ChatKeyGenerator.generateChatMessageKey(snowflakeId);
        String chatRoomMessageKey = ChatKeyGenerator.generateChatRoomMessageKey(chatRoomId);
        String messageJson = toValueJson(redisChatMessage);
        String memberJson = toValueJson(RedisChatRoomMessage.from(snowflakeId));
        String scoreText = BigDecimal.valueOf(Double.parseDouble(snowflakeId)).toPlainString();

        redisTemplate.execute(PUBLISH_SCRIPT, ARGS_SERIALIZER, RESULT_SERIALIZER,
                List.of(chatMessageKey, chatRoomMessageKey),
                messageJson, scoreText, memberJson);
    }

    @SuppressWarnings("unchecked")
    private String toValueJson(Object value) {
        RedisSerializer<Object> valueSerializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        byte[] bytes = valueSerializer.serialize(value);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Recover
    public void recover(Exception e, Long chatRoomId, RedisChatMessage redisChatMessage) {
        log.error("failed to save chat message {} of chatRoom {}", redisChatMessage.id(), chatRoomId, e);
        throw new CustomException(Status.PUBLISH_CHAT_MESSAGE_FAIL);
    }
}
