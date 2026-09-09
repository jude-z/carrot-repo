package jude.carrot.chatserver.response;

import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;

import java.util.List;

public record PollingChatMessagesResponse(
        List<ChatMessageElement> elements,
        int elementCount
) {

    public PollingChatMessagesResponse {
        elements = elements == null ? List.of() : List.copyOf(elements);
        elementCount = elements.size();
    }

    public PollingChatMessagesResponse(List<ChatMessageElement> elements) {
        this(elements, 0);
    }

    public static PollingChatMessagesResponse from(List<RedisChatMessage> redisChatMessages) {
        List<ChatMessageElement> chatMessageElements = redisChatMessages.stream()
                .map(ChatMessageElement::from)
                .toList();
        return new PollingChatMessagesResponse(chatMessageElements);
    }
}
