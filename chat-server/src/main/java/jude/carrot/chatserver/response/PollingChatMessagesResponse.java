package jude.carrot.chatserver.response;

import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

@Builder
public record PollingChatMessagesResponse(
        List<ChatMessageElement> elements,
        int elementCount
) {

    public PollingChatMessagesResponse {
        if (elements == null) {
            elements = new ArrayList<>();
        }
    }

    public static PollingChatMessagesResponse from(List<RedisChatMessage> redisChatMessages) {
        List<ChatMessageElement> chatMessageElements = redisChatMessages.stream()
                .map(redisChatMessage -> ChatMessageElement.builder()
                        .id(redisChatMessage.id())
                        .content(redisChatMessage.content())
                        .publishedBy(redisChatMessage.publishedBy())
                        .publishedAt(redisChatMessage.publishedAt())
                        .build()
                )
                .toList();

        return PollingChatMessagesResponse
                .builder()
                .elements(chatMessageElements)
                .elementCount(chatMessageElements.size())
                .build();
    }
}
