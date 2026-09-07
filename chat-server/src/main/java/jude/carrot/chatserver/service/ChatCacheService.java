package jude.carrot.chatserver.service;

import jude.carrot.chatserver.key.redis.ChatKeyGenerator;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.repository.chat.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ChatCacheService {

    private final ChatRepository chatRepository;

    @Cacheable(cacheNames = "chatRoom", key = "#chatRoomId", unless = "#result == null")
    public Optional<ChatRoom> fetchChatRoom(Long chatRoomId){
        return chatRepository.fetchChatRoom(chatRoomId);
    }

    @Cacheable(cacheNames = "chatParticipant", key = "#chatRoomId + '::' + #userId", unless = "#result == null")
    public Optional<ChatParticipant> fetchChatParticipant(Long chatRoomId, Long userId){
        return chatRepository.fetchChatParticipant(chatRoomId, userId);
    }

    @Cacheable(cacheNames = "chatMessage", key = "#chatMessageKey", unless = "#result == null")
    public Optional<ChatMessage> fetchChatMessage(String chatMessageKey){
        String chatMessageId = ChatKeyGenerator.parseChatMessageId(chatMessageKey);
        return chatRepository.fetchChatMessage(chatMessageId);
    }
}
