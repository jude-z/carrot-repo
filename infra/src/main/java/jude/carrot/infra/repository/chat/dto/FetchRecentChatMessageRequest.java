package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

@Builder
public record FetchRecentChatMessageRequest(
        String content
) {
}
