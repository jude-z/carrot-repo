package jude.carrot.chatserver.service;

import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.repository.chat.ChatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatCacheServiceTest {

    @InjectMocks
    private ChatCacheService chatCacheService;

    @Mock
    private ChatRepository chatRepository;

    @Test
    @DisplayName("fetchChatRoom은 chatRepository의 조회 결과를 그대로 반환한다")
    void fetchChatRoom_delegatesToRepository() {
        ChatRoom chatRoom = ChatRoom.builder().id(1L).build();
        when(chatRepository.fetchChatRoom(1L)).thenReturn(Optional.of(chatRoom));

        assertThat(chatCacheService.fetchChatRoom(1L)).contains(chatRoom);
    }

    @Test
    @DisplayName("fetchChatRoom은 존재하지 않으면 빈 Optional을 반환한다")
    void fetchChatRoom_returnsEmptyWhenNotFound() {
        when(chatRepository.fetchChatRoom(1L)).thenReturn(Optional.empty());

        assertThat(chatCacheService.fetchChatRoom(1L)).isEmpty();
    }

    @Test
    @DisplayName("fetchChatParticipant는 chatRepository의 조회 결과를 그대로 반환한다")
    void fetchChatParticipant_delegatesToRepository() {
        ChatParticipant chatParticipant = ChatParticipant.builder().id(1L).build();
        when(chatRepository.fetchChatParticipant(1L, 1L)).thenReturn(Optional.of(chatParticipant));

        assertThat(chatCacheService.fetchChatParticipant(1L, 1L)).contains(chatParticipant);
    }

    @Test
    @DisplayName("fetchChatMessage는 chatMessageKey에서 id를 추출해 조회한다")
    void fetchChatMessage_parsesKeyAndDelegatesToRepository() {
        ChatMessage chatMessage = ChatMessage.builder().id("1").build();
        when(chatRepository.fetchChatMessage("1")).thenReturn(Optional.of(chatMessage));

        assertThat(chatCacheService.fetchChatMessage("chatMessage::1")).contains(chatMessage);
        verify(chatRepository).fetchChatMessage("1");
    }
}
