package jude.carrot.infra.repository.chat;

import jude.carrot.infra.InfraTestConfig;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.entity.chat.ReadStatus;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.fixture.chat.ChatFactory;
import jude.carrot.infra.fixture.user.UserFactory;
import jude.carrot.infra.repository.chat.jpa.ChatMessageJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatParticipantJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatRoomJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ReadStatusJpaRepository;
import jude.carrot.infra.repository.user.jpa.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create")
@ContextConfiguration(classes = InfraTestConfig.class)
@Testcontainers
@Transactional
@Import(ChatRepositoryTest.ChatRepositoryConfig.class)
class ChatRepositoryTest {

    @TestConfiguration
    static class ChatRepositoryConfig {
        @Bean
        ChatRepository chatRepository(ChatMessageJpaRepository chatMessageJpaRepository,
                                       ChatParticipantJpaRepository chatParticipantJpaRepository,
                                       ChatRoomJpaRepository chatRoomJpaRepository,
                                       ReadStatusJpaRepository readStatusJpaRepository) {
            return new ChatRepositoryImpl(chatMessageJpaRepository, chatParticipantJpaRepository,
                    chatRoomJpaRepository, readStatusJpaRepository);
        }
    }

    static final String DATABASE_NAME = "test";
    static final String DATABASE_USERNAME = "test";
    static final String DATABASE_PASSWORD = UUID.randomUUID().toString();

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USERNAME)
            .withPassword(DATABASE_PASSWORD);

    @Autowired
    ChatRepository chatRepository;
    @Autowired
    UserJpaRepository userJpaRepository;
    @Autowired
    TestEntityManager entityManager;

    User creatorUser;
    User opponentUser;
    ChatParticipant creator;
    ChatParticipant opponent;

    @BeforeEach
    void setUp() {
        creatorUser = userJpaRepository.save(UserFactory.create("creator@carrot.com"));
        opponentUser = userJpaRepository.save(UserFactory.create("opponent@carrot.com"));
        creator = ChatFactory.createParticipant(creatorUser);
        opponent = ChatFactory.createParticipant(opponentUser);
        chatRepository.saveAll(creator, opponent);
    }

    @Test
    @DisplayName("채팅 참여자를 여러 명 저장하면 각각 id가 채번된다")
    void saveAllAssignsIdToEachParticipant() {
        assertThat(creator.getId()).isNotNull();
        assertThat(opponent.getId()).isNotNull();
    }

    @Test
    @DisplayName("채팅방을 저장하면 id가 채번된다")
    void saveChatRoomAssignsId() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);

        chatRepository.save(chatRoom);

        assertThat(chatRoom.getId()).isNotNull();
    }

    @Test
    @DisplayName("채팅 메시지를 저장하면 id가 채번된다")
    void saveChatMessageAssignsId() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatFactory.createChatMessage("hello", chatRoom, creator);

        chatRepository.save(chatMessage);

        assertThat(chatMessage.getId()).isNotNull();
    }

    @Test
    @DisplayName("읽음 상태를 저장하면 id가 채번된다")
    void saveReadStatusAssignsId() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatFactory.createChatMessage("hello", chatRoom, creator);
        chatRepository.save(chatMessage);
        ReadStatus readStatus = ChatFactory.createReadStatus(opponent, chatMessage);

        chatRepository.save(readStatus);

        assertThat(readStatus.getId()).isNotNull();
    }

    @Test
    @DisplayName("채팅방 생성자가 조회하면 채팅방이 반환된다")
    void fetchChatRoomByUserIdAndChatRoomIdSuccessWhenCreator() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatRoom> found = chatRepository.fetchChatRoomByUserIdAndChatRoomId(creatorUser.getId(), chatRoom.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(c -> assertThat(c.getTitle()).isEqualTo("title"));
    }

    @Test
    @DisplayName("채팅방 상대방이 조회해도 채팅방이 반환된다")
    void fetchChatRoomByUserIdAndChatRoomIdSuccessWhenOpponent() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);
        chatRepository.save(chatRoom);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatRoom> found = chatRepository.fetchChatRoomByUserIdAndChatRoomId(opponentUser.getId(), chatRoom.getId());

        assertThat(found).isPresent();
    }

    @Test
    @DisplayName("채팅방과 무관한 회원이 조회하면 비어있다")
    void fetchChatRoomByUserIdAndChatRoomIdFailWhenNotParticipant() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);
        chatRepository.save(chatRoom);
        User strangerUser = userJpaRepository.save(UserFactory.create("stranger@carrot.com"));
        entityManager.flush();
        entityManager.clear();

        Optional<ChatRoom> found = chatRepository.fetchChatRoomByUserIdAndChatRoomId(strangerUser.getId(), chatRoom.getId());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("채팅방의 메시지를 join fetch로 조회하면 채팅방과 작성자가 함께 조회된다")
    void joinFetchChatMessageSuccess() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage chatMessage = ChatFactory.createChatMessage("hello", chatRoom, creator);
        chatRepository.save(chatMessage);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatMessage> found = chatRepository.joinFetchChatMessage(chatRoom.getId());

        assertThat(found).isPresent();
        ChatMessage fetched = found.get();
        assertThat(fetched.getContent()).isEqualTo("hello");
        assertThat(fetched.getChatRoom().getId()).isEqualTo(chatRoom.getId());
        assertThat(fetched.getPublishedBy().getId()).isEqualTo(creator.getId());
    }

    @Test
    @DisplayName("채팅방에 메시지가 여러 건이면 가장 최근에 저장된 메시지만 조회된다")
    void joinFetchChatMessageSuccessReturnsMostRecentOnly() {
        ChatRoom chatRoom = ChatFactory.createChatRoom("title", creator, opponent);
        chatRepository.save(chatRoom);
        ChatMessage firstMessage = ChatFactory.createChatMessage("first", chatRoom, creator);
        chatRepository.save(firstMessage);
        ChatMessage secondMessage = ChatFactory.createChatMessage("second", chatRoom, opponent);
        chatRepository.save(secondMessage);
        entityManager.flush();
        entityManager.clear();

        Optional<ChatMessage> found = chatRepository.joinFetchChatMessage(chatRoom.getId());

        assertThat(found).isPresent()
                .hasValueSatisfying(c -> assertThat(c.getContent()).isEqualTo("second"));
    }

    @Test
    @DisplayName("메시지가 없는 채팅방을 조회하면 비어있다")
    void joinFetchChatMessageFailWhenNotExist() {
        Optional<ChatMessage> found = chatRepository.joinFetchChatMessage(-1L);

        assertThat(found).isEmpty();
    }
}
