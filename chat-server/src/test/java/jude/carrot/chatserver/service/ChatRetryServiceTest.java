package jude.carrot.chatserver.service;

import jude.carrot.chatserver.config.RetryConfig;
import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.infra.repository.chat.dto.PublishChatRequest;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import jude.carrot.service.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static jude.carrot.service.status.Status.PUBLISH_CHAT_MESSAGE_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {ChatRetryService.class, RetryConfig.class})
class ChatRetryServiceTest {

    private static final Long CHAT_ROOM_ID = 5L;
    private static final String SNOWFLAKE_ID = "7502908589560827910";

    @Autowired
    private ChatRetryService chatRetryService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        RedisSerializer<Object> valueSerializer = new JacksonJsonRedisSerializer<>(Object.class);
        doReturn(valueSerializer).when(redisTemplate).getValueSerializer();
    }

    private RedisChatMessage redisChatMessage() {
        return RedisChatMessage.from(SNOWFLAKE_ID, new PublishChatRequest("hello"), LocalDateTime.of(2026, 9, 8, 10, 0), 7L);
    }

    @Test
    @DisplayName("메시지 ID로 키/멤버/score를 구성해 SET 과 ZADD 를 Lua 스크립트 한 번으로 실행하고, 값은 value serializer 형식(JSON)으로 넘긴다")
    void saveRedis_executesSingleScript() {
        doReturn(1L).when(redisTemplate).execute(any(RedisScript.class), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any(Object[].class));

        chatRetryService.saveRedis(CHAT_ROOM_ID, redisChatMessage());

        ArgumentCaptor<List<String>> keys = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
        verify(redisTemplate).execute(eq(ChatRetryService.PUBLISH_SCRIPT), any(RedisSerializer.class), any(RedisSerializer.class), keys.capture(), args.capture());
        assertThat(keys.getValue()).containsExactly(
                ChatKeyGenerator.generateChatMessageKey(SNOWFLAKE_ID),
                ChatKeyGenerator.generateChatRoomMessageKey(CHAT_ROOM_ID));
        Object[] argv = args.getValue();
        assertThat(argv).hasSize(3);
        assertThat((String) argv[0]).contains("\"content\":\"hello\"").doesNotStartWith("\"");
        assertThat((String) argv[1]).isEqualTo("7502908589560828000").doesNotContain("E");
        assertThat((String) argv[2]).contains(SNOWFLAKE_ID);
        assertThat(ChatRetryService.PUBLISH_SCRIPT.getScriptAsString()).contains("SET").contains("ZADD");
    }

    @Test
    @DisplayName("연결 실패(RedisConnectionFailureException)가 반복되면 재시도 후 CustomException(PUBLISH_CHAT_MESSAGE_FAIL)을 던진다")
    void saveRedis_recoversAfterRetriesOnConnectionFailure() {
        doThrow(new RedisConnectionFailureException("connection refused")).when(redisTemplate)
                .execute(any(RedisScript.class), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any(Object[].class));

        assertThatThrownBy(() -> chatRetryService.saveRedis(CHAT_ROOM_ID, redisChatMessage()))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(PUBLISH_CHAT_MESSAGE_FAIL.getHttpStatus());

        verify(redisTemplate, times(3)).execute(any(RedisScript.class), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any(Object[].class));
    }

    @Test
    @DisplayName("타임아웃(QueryTimeoutException)이 반복되면 재시도 후 CustomException(PUBLISH_CHAT_MESSAGE_FAIL)을 던진다")
    void saveRedis_recoversAfterRetriesOnTimeout() {
        doThrow(new QueryTimeoutException("timeout")).when(redisTemplate)
                .execute(any(RedisScript.class), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any(Object[].class));

        assertThatThrownBy(() -> chatRetryService.saveRedis(CHAT_ROOM_ID, redisChatMessage()))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(PUBLISH_CHAT_MESSAGE_FAIL.getHttpStatus());

        verify(redisTemplate, times(3)).execute(any(RedisScript.class), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any(Object[].class));
    }

    @Test
    @DisplayName("일시 실패 후 재시도에 성공하면 예외 없이 저장을 마친다")
    void saveRedis_succeedsOnRetry() {
        doThrow(new RedisConnectionFailureException("connection refused")).doReturn(1L).when(redisTemplate)
                .execute(any(RedisScript.class), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any(Object[].class));

        chatRetryService.saveRedis(CHAT_ROOM_ID, redisChatMessage());

        verify(redisTemplate, times(2)).execute(any(RedisScript.class), any(RedisSerializer.class), any(RedisSerializer.class), anyList(), any(Object[].class));
    }
}
