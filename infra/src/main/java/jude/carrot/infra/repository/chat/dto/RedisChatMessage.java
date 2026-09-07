package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Builder
public record RedisChatMessage(
        String id,
        String content,
        Long publishedBy,
        LocalDateTime publishedAt
) {

    public static RedisChatMessage from(String id, PublishChatRequest publishChatRequest, LocalDateTime publishedAt, Long publishedBy) {
        return RedisChatMessage.builder()
                .id(id)
                .content(publishChatRequest.content())
                .publishedBy(publishedBy)
                .publishedAt(publishedAt)
                .build();
    }
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /**
     * 값은 JacksonJsonRedisSerializer(Object.class)로 저장되어 읽을 때 Map으로 역직렬화된다.
     * 그 Map을 RedisChatMessage로 변환한다.
     */
    public static RedisChatMessage of(Object value) {
        return MAPPER.convertValue(value, RedisChatMessage.class);
    }
}
