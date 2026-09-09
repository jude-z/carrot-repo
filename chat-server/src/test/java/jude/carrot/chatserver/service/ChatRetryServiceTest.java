package jude.carrot.chatserver.service;

import jude.carrot.chatserver.config.RetryConfig;
import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.infra.repository.chat.dto.RedisChatRoomMessage;
import jude.carrot.service.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static jude.carrot.service.status.Status.PUBLISH_CHAT_MESSAGE_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {ChatRetryService.class, RetryConfig.class})
class ChatRetryServiceTest {

    private static final Long CHAT_ROOM_ID = 10L;
    private static final String SNOWFLAKE_ID = "123456789";

    @Autowired
    private ChatRetryService chatRetryService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private RedisChatMessage redisChatMessage() {
        return RedisChatMessage.builder()
                .id(SNOWFLAKE_ID).content("hello").publishedBy(100L).publishedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("메시지 ID로 chatMessage 키, chatRoom zset 키, 멤버, score를 구성해 MULTI/EXEC 안에서 저장한다")
    void saveRedis_buildsKeysAndSavesInTransaction() {
        RedisOperations<String, Object> operations = mock(RedisOperations.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        ZSetOperations<String, Object> zSetOperations = mock(ZSetOperations.class);
        when(operations.opsForValue()).thenReturn(valueOperations);
        when(operations.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenAnswer(invocation -> invocation.<SessionCallback<List<Object>>>getArgument(0).execute(operations));
        RedisChatMessage message = redisChatMessage();

        chatRetryService.saveRedis(CHAT_ROOM_ID, message);

        ArgumentCaptor<RedisChatRoomMessage> memberCaptor = ArgumentCaptor.forClass(RedisChatRoomMessage.class);
        verify(operations).multi();
        verify(valueOperations).set(ChatKeyGenerator.generateChatMessageKey(SNOWFLAKE_ID), message);
        verify(zSetOperations).add(eq(ChatKeyGenerator.generateChatRoomMessageKey(CHAT_ROOM_ID)),
                memberCaptor.capture(), eq(123456789.0));
        verify(operations).exec();
        assertThat(memberCaptor.getValue().chatMessageId()).isEqualTo(SNOWFLAKE_ID);
    }

    @Test
    @DisplayName("연결 실패(RedisConnectionFailureException)가 반복되면 재시도 후 CustomException(PUBLISH_CHAT_MESSAGE_FAIL)을 던진다")
    void saveRedis_recoversAfterRetriesOnConnectionFailure() {
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        assertThatThrownBy(() -> chatRetryService.saveRedis(CHAT_ROOM_ID, redisChatMessage()))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(PUBLISH_CHAT_MESSAGE_FAIL.getHttpStatus());

        verify(redisTemplate, times(3)).execute(any(SessionCallback.class));
    }

    @Test
    @DisplayName("타임아웃(QueryTimeoutException)이 반복되면 재시도 후 CustomException(PUBLISH_CHAT_MESSAGE_FAIL)을 던진다")
    void saveRedis_recoversAfterRetriesOnTimeout() {
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenThrow(new QueryTimeoutException("timeout"));

        assertThatThrownBy(() -> chatRetryService.saveRedis(CHAT_ROOM_ID, redisChatMessage()))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(PUBLISH_CHAT_MESSAGE_FAIL.getHttpStatus());

        verify(redisTemplate, times(3)).execute(any(SessionCallback.class));
    }

    @Test
    @DisplayName("일시 실패 후 재시도에 성공하면 예외 없이 저장을 마친다")
    void saveRedis_succeedsOnRetry() {
        when(redisTemplate.execute(any(SessionCallback.class)))
                .thenThrow(new RedisConnectionFailureException("connection refused"))
                .thenReturn(List.of());

        chatRetryService.saveRedis(CHAT_ROOM_ID, redisChatMessage());

        verify(redisTemplate, times(2)).execute(any(SessionCallback.class));
    }
}
