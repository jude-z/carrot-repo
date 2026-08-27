package jude.carrot.infra.repository.chat;


import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;import jude.carrot.infra.entity.chat.ReadStatus;
import jude.carrot.infra.repository.chat.dto.ChatMessageBulk;
import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.chat.dto.ChatRoomMessageBulk;
import jude.carrot.infra.repository.chat.dto.ReadStatusBulk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ChatRepository {
    void save(ChatRoom chatRoom);
    void saveAll(ChatParticipant... chatParticipants);
    void save(ChatMessage chatMessage);
    void save(ReadStatus readStatus);
    void bulkReadStatus(List<ReadStatusBulk> readStatuses);
    void bulkChatRoomMessage(List<ChatRoomMessageBulk> chatRoomMessageBulks);
    void bulkChatMessage(List<ChatMessageBulk> chatMessageBulks);
    Optional<ChatRoom> fetchChatRoomByUserIdAndChatRoomId(Long userId, Long chatRoomId);
    Optional<ChatMessage> joinFetchChatMessage(Long chatRoomId);
    Page<ChatMessageElement> fetch(Pageable pageable, Long chatRoomId);
    Optional<ChatRoom> fetchChatRoom(Long chatRoomId);
    Optional<ChatParticipant> fetchChatParticipant(Long chatRoomId, Long userId);
    Optional<ChatMessage> fetchChatMessage(String chatMessageId);
}
