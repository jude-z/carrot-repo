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
    public static String chatMessageIdOf(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("unexpected readStatus value type: " + value);
        }
        return String.valueOf(map.get("chatMessageId"));
    }
}
