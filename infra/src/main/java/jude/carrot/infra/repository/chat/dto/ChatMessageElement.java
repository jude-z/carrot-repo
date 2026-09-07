package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatMessageElement(
        String id,
        String content,
        Long publishedBy,
        LocalDateTime publishedAt
) {

    public static ChatMessageElement from(RedisChatMessage redisChatMessage) {
        return ChatMessageElement.builder()
                .id(redisChatMessage.id())
                .content(redisChatMessage.content())
                .publishedBy(redisChatMessage.publishedBy())
                .publishedAt(redisChatMessage.publishedAt())
                .build();
    }
}
