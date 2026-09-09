package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record RedisChatRoomMessage(
        String chatMessageId
) {

    public static RedisChatRoomMessage from(String chatMessageId) {
        return RedisChatRoomMessage.builder()
                .chatMessageId(chatMessageId)
                .build();
    }

    public static String chatMessageIdOf(Object member) {
        if (!(member instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("unexpected zset member type: " + member);
        }
        return String.valueOf(map.get("chatMessageId"));
    }
}
