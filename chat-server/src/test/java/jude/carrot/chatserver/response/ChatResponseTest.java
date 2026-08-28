package jude.carrot.chatserver.response;

import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import jude.carrot.infra.repository.chat.dto.RedisChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ChatResponseTest {

    @Test
    @DisplayName("chatRoomId로 CreateChatRoomResponse를 생성한다")
    void createChatRoomResponse_from() {
        CreateChatRoomResponse response = CreateChatRoomResponse.from(10L);

        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("ChatMessage 엔티티로부터 발행자 정보를 포함한 FetchRecentChatMessage를 생성한다")
    void fetchRecentChatMessage_from() {
        User user = User.from("carrot@carrot.com", "encoded", "carrot", (SingleImage) null);
        user.setId(1L);
        ChatParticipant publisher = ChatParticipant.from(user);
        ChatMessage chatMessage = ChatMessage.builder()
                .id("100")
                .content("hello")
                .publishedBy(publisher)
                .publishedAt(LocalDateTime.now())
                .build();

        FetchRecentChatMessage response = FetchRecentChatMessage.from(chatMessage);

        assertThat(response.id()).isEqualTo("100");
        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.publishedById()).isEqualTo(1L);
        assertThat(response.publishedByEmail()).isEqualTo("carrot@carrot.com");
        assertThat(response.publishedByNickname()).isEqualTo("carrot");
    }

    @Test
    @DisplayName("RedisChatMessage 목록으로부터 PollingChatMessagesResponse를 생성한다")
    void pollingChatMessagesResponse_from() {
        RedisChatMessage message1 = RedisChatMessage.builder()
                .id("1").content("hi").publishedBy(1L).publishedAt(LocalDateTime.now()).build();
        RedisChatMessage message2 = RedisChatMessage.builder()
                .id("2").content("hello").publishedBy(2L).publishedAt(LocalDateTime.now()).build();

        PollingChatMessagesResponse response = PollingChatMessagesResponse.from(List.of(message1, message2));

        assertThat(response.elementCount()).isEqualTo(2);
        assertThat(response.elements())
                .extracting("id", "content", "publishedBy")
                .containsExactly(
                        tuple("1", "hi", 1L),
                        tuple("2", "hello", 2L)
                );
    }

    @Test
    @DisplayName("RedisChatMessage 목록이 비어있으면 빈 elements를 갖는 응답을 생성한다")
    void pollingChatMessagesResponse_from_whenEmpty() {
        PollingChatMessagesResponse response = PollingChatMessagesResponse.from(List.of());

        assertThat(response.elements()).isEmpty();
        assertThat(response.elementCount()).isZero();
    }

    @Test
    @DisplayName("builder로 elements를 지정하지 않으면 빈 리스트가 기본값이 된다")
    void pollingChatMessagesResponse_defaultElementsIsEmptyList() {
        PollingChatMessagesResponse response = PollingChatMessagesResponse.builder()
                .elementCount(0)
                .build();

        assertThat(response.elements()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Page<ChatMessageElement>로부터 ChatMessageResponse를 생성한다")
    void chatMessageResponse_from() {
        ChatMessageElement element = ChatMessageElement.builder()
                .id("1").content("hi").publishedBy(1L).publishedAt(LocalDateTime.now()).build();
        Page<ChatMessageElement> page = new PageImpl<>(List.of(element), PageRequest.of(0, 20), 1);

        ChatMessageResponse response = ChatMessageResponse.from(page);

        assertThat(response.page()).containsExactly(element);
        assertThat(response.pageSize()).isEqualTo(20);
        assertThat(response.pageNum()).isZero();
        assertThat(response.elementCount()).isEqualTo(1);
        assertThat(response.totalPage()).isEqualTo(1);
        assertThat(response.isLast()).isTrue();
    }
}
