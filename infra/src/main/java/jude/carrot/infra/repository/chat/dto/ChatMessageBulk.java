package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatMessageBulk(
        String id,
        String content,
        Long publishedById,
        LocalDateTime publishedAt
) {

    public static ChatMessageBulk from(String id, RedisChatMessage redisChatMessage) {
        return ChatMessageBulk.builder()
                .id(id)
                .content(redisChatMessage.content())
                .publishedById(redisChatMessage.publishedBy())
                .publishedAt(redisChatMessage.publishedAt())
                .build();
    }
}
