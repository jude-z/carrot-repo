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
}
