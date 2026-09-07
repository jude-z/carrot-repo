package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

@Builder
public record ChatRoomMessageBulk(
        Long chatRoomId,
        String chatMessageId
) {

    public static ChatRoomMessageBulk from(Long chatRoomId, String chatMessageId) {
        return ChatRoomMessageBulk.builder()
                .chatRoomId(chatRoomId)
                .chatMessageId(chatMessageId)
                .build();
    }
}
