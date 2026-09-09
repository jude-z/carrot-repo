package jude.carrot.chatserver.response;

import lombok.Builder;

@Builder
public record CreateChatRoomResponse(
        Long id
) {

    public static CreateChatRoomResponse from(Long id) {
        return CreateChatRoomResponse.builder()
                .id(id)
                .build();
    }
}
