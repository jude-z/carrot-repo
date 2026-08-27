package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

@Builder
public record RedisChatRoomMessage(
        String chatMessageId
) {

    public static RedisChatRoomMessage from(String chatMessageId) {
        return RedisChatRoomMessage.builder()
                .chatMessageId(chatMessageId)
                .build();
    }
}
