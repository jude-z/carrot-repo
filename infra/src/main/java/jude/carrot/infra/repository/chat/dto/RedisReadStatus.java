package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record RedisReadStatus(
        String chatMessageId
) {

    public static RedisReadStatus from(String chatMessageId) {
        return RedisReadStatus.builder()
                .chatMessageId(chatMessageId)
                .build();
    }
    /**
     * 값은 JacksonJsonRedisSerializer(Object.class)로 저장되어 읽을 때 Map으로 역직렬화된다.
     * 그 Map에서 chatMessageId를 꺼낸다.
     */
    public static String chatMessageIdOf(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("unexpected readStatus value type: " + value);
        }
        return String.valueOf(map.get("chatMessageId"));
    }
}
