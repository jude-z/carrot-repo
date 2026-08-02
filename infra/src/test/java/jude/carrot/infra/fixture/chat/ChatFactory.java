package jude.carrot.infra.fixture.chat;

import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.entity.chat.ReadStatus;
import jude.carrot.infra.entity.user.User;

public class ChatFactory {

    private ChatFactory() {
    }

    public static ChatParticipant createParticipant(User user) {
        return ChatParticipant.from(user);
    }

    public static ChatRoom createChatRoom(String title, ChatParticipant creator, ChatParticipant opponent) {
        return ChatRoom.from(title, creator, opponent);
    }

    public static ChatMessage createChatMessage(String content, ChatRoom chatRoom, ChatParticipant publishedBy) {
        return ChatMessage.builder()
                .content(content)
                .chatRoom(chatRoom)
                .publishedBy(publishedBy)
                .build();
    }

    public static ReadStatus createReadStatus(ChatParticipant chatParticipant, ChatMessage chatMessage) {
        return ReadStatus.from(chatParticipant, chatMessage);
    }
}
