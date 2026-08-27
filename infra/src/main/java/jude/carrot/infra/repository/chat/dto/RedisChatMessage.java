package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

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
}
