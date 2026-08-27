package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

@Builder
public record CreateChatRoomRequest(
        Long opponentId,
        String title
) {
}
