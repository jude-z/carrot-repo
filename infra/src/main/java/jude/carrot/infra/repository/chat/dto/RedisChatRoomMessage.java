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

    /**
     * zset 멤버는 JacksonJsonRedisSerializer(Object.class)로 저장되어
     * 읽을 때 Map(LinkedHashMap)으로 역직렬화된다. 그 Map에서 chatMessageId를 꺼낸다.
     */
    public static String chatMessageIdOf(Object member) {
        if (!(member instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("unexpected zset member type: " + member);
        }
        return String.valueOf(map.get("chatMessageId"));
    }
}
