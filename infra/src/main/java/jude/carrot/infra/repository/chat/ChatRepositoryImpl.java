package jude.carrot.infra.repository.chat;

import jude.carrot.infra.repository.chat.jpa.ChatMessageJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatParticipantJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatRoomJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ReadStatusJpaRepository;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.entity.chat.ReadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;import java.util.Arrays;import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class ChatRepositoryImpl implements ChatRepository {
    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final ChatParticipantJpaRepository chatParticipantJpaRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ReadStatusJpaRepository readStatusJpaRepository;


    @Override
    public void save(ChatRoom chatRoom) {
        chatRoomJpaRepository.save(chatRoom);
    }@Override
    public void saveAll(ChatParticipant... chatParticipants) {
        chatParticipantJpaRepository.saveAll(Arrays.asList(chatParticipants));
    }



    @Override
    public void save(ChatMessage chatMessage) {
        chatMessageJpaRepository.save(chatMessage);
    }

    @Override
    public void save(ReadStatus readStatus) {
        readStatusJpaRepository.save(readStatus);
    }@Override
    public Optional<ChatRoom> fetchChatRoomByUserIdAndChatRoomId(Long userId, Long chatRoomId) {
        return chatRoomJpaRepository.findByUserAndChatRoom(userId, chatRoomId);
    }@Override
    public Optional<ChatMessage> joinFetchChatMessage(Long chatRoomId) {
        return chatMessageJpaRepository.joinFetchChatMessage(chatRoomId);
    }
}
