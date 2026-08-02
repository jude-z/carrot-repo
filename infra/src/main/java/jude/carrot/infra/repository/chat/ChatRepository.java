package jude.carrot.infra.repository.chat;


import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;import jude.carrot.infra.entity.chat.ReadStatus;import java.util.Optional;

public interface ChatRepository {
    void save(ChatRoom chatRoom);
    void saveAll(ChatParticipant... chatParticipants);
    void save(ChatMessage chatMessage);
    void save(ReadStatus readStatus);

    Optional<ChatRoom> fetchChatRoomByUserIdAndChatRoomId(Long userId, Long chatRoomId);
    Optional<ChatMessage> joinFetchChatMessage(Long chatRoomId);
}
