package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

@Builder
public record RedisReadStatus(
        String chatMessageId
) {

    public static RedisReadStatus from(String chatMessageId) {
        return RedisReadStatus.builder()
                .chatMessageId(chatMessageId)
                .build();
    }
}
