package jude.carrot.chatserver.response;

import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.user.User;
import lombok.Builder;

@Builder
public record FetchRecentChatMessage(
        String id,
        String content,
        Long publishedById,
        String publishedByEmail,
        String publishedByNickname
) {

    public static FetchRecentChatMessage from(ChatMessage chatMessage) {
        User publisher = chatMessage.getPublishedBy().getUser();
        return FetchRecentChatMessage.builder()
                .id(chatMessage.getId())
                .content(chatMessage.getContent())
                .publishedById(publisher.getId())
                .publishedByEmail(publisher.getEmail())
                .publishedByNickname(publisher.getNickname())
                .build();
    }
}
