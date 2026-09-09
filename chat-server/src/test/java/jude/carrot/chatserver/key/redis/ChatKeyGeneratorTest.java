package jude.carrot.chatserver.key.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatKeyGeneratorTest {

    @Test
    @DisplayName("채팅 메시지 키는 snowflakeId별로 서로 다른 값을 생성한다")
    void generateChatMessageKey_isUniquePerSnowflakeId() {
        String key1 = ChatKeyGenerator.generateChatMessageKey("111");
        String key2 = ChatKeyGenerator.generateChatMessageKey("222");

        assertThat(key1).isEqualTo("chatMessage::111");
        assertThat(key2).isEqualTo("chatMessage::222");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("채팅방 메시지 키는 chatRoomId별로 서로 다른 값을 생성한다")
    void generateChatRoomMessageKey_isUniquePerChatRoomId() {
        String key1 = ChatKeyGenerator.generateChatRoomMessageKey(1L);
        String key2 = ChatKeyGenerator.generateChatRoomMessageKey(2L);

        assertThat(key1).isEqualTo("chatRoom::1");
        assertThat(key2).isEqualTo("chatRoom::2");
        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("읽음 상태 키는 userId/chatRoomId 조합별로 서로 다른 값을 생성한다")
    void generateReadStatusKey_isUniquePerUserAndChatRoom() {
        String key1 = ChatKeyGenerator.generateReadStatusKey(1L, 10L);
        String key2 = ChatKeyGenerator.generateReadStatusKey(1L, 20L);
        String key3 = ChatKeyGenerator.generateReadStatusKey(2L, 10L);

        assertThat(key1).isEqualTo("chatParticipantId::1::10");
        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isNotEqualTo(key3);
    }

    @Test
    @DisplayName("채팅 메시지 패턴은 chatMessage:: 접두사와 와일드카드로 구성된다")
    void chatMessagePattern_matchesAnyChatMessageKeyKey() {
        assertThat(ChatKeyGenerator.chatMessageKeyPattern()).isEqualTo("chatMessage::*");
        assertThat(ChatKeyGenerator.generateChatMessageKey("999"))
                .matches(ChatKeyGenerator.chatMessageKeyPattern().replace("*", ".*"));
    }

    @Test
    @DisplayName("채팅방 메시지 패턴은 chatRoom:: 접두사와 와일드카드로 구성된다")
    void chatRoomMessagePattern_matchesAnyChatRoomMessageKeyKey() {
        assertThat(ChatKeyGenerator.chatRoomMessageKeyPattern()).isEqualTo("chatRoom::*");
        assertThat(ChatKeyGenerator.generateChatRoomMessageKey(5L))
                .matches(ChatKeyGenerator.chatRoomMessageKeyPattern().replace("*", ".*"));
    }

    @Test
    @DisplayName("읽음 상태 패턴은 chatParticipantId:: 접두사와 두 개의 와일드카드로 구성된다")
    void readStatusPattern_matchesAnyReadStatusKeyKey() {
        assertThat(ChatKeyGenerator.readStatusKeyPattern()).isEqualTo("chatParticipantId::*::*");
        assertThat(ChatKeyGenerator.generateReadStatusKey(3L, 30L))
                .matches(ChatKeyGenerator.readStatusKeyPattern().replace("*", ".*"));
    }
}
