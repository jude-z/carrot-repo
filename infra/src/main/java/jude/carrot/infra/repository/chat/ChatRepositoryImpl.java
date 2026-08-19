package jude.carrot.infra.repository.chat;

import jude.carrot.infra.repository.chat.dto.ChatDto;
import jude.carrot.infra.repository.chat.dto.ChatDto.ReadStatusBulk;
import jude.carrot.infra.repository.chat.jpa.ChatMessageJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatParticipantJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ChatRoomJpaRepository;
import jude.carrot.infra.repository.chat.jpa.ReadStatusJpaRepository;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import jude.carrot.infra.entity.chat.ReadStatus;
import jude.carrot.infra.repository.sql.SqlGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static jude.carrot.infra.repository.chat.dto.ChatDto.*;


@Repository
public class ChatRepositoryImpl implements ChatRepository {
    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final ChatParticipantJpaRepository chatParticipantJpaRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ReadStatusJpaRepository readStatusJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    public ChatRepositoryImpl(ChatMessageJpaRepository chatMessageJpaRepository,
                              ChatParticipantJpaRepository chatParticipantJpaRepository,
                              ChatRoomJpaRepository chatRoomJpaRepository,
                              ReadStatusJpaRepository readStatusJpaRepository,
                              DataSource dataSource) {
        this.chatMessageJpaRepository = chatMessageJpaRepository;
        this.chatParticipantJpaRepository = chatParticipantJpaRepository;
        this.chatRoomJpaRepository = chatRoomJpaRepository;
        this.readStatusJpaRepository = readStatusJpaRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(ChatRoom chatRoom) {
        chatRoomJpaRepository.save(chatRoom);
    }
    @Override
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
    }

    @Override
    public void bulkReadStatus(List<ReadStatusBulk> readStatuses) {
        String sql = SqlGenerator.bulkReadStatueSql();
        jdbcTemplate.batchUpdate(sql, readStatuses,readStatuses.size(),
                (ps, readStatus) -> {
                    ps.setLong(1, readStatus.getChatParticipantId());
                    ps.setString(2, readStatus.getChatMessageId());
                    ps.setTimestamp(3, Timestamp.valueOf(readStatus.getCreateTime()));
                    ps.setTimestamp(4, Timestamp.valueOf(readStatus.getUpdateTime()));
                }
            );
    }

    @Override
    public void bulkChatRoomMessage(List<ChatRoomMessageBulk> chatRoomMessageBulks) {
        String sql = SqlGenerator.bulkChatRoomMessageSql();
        jdbcTemplate.batchUpdate(sql, chatRoomMessageBulks, chatRoomMessageBulks.size(),
                (ps, chatRoomMessage) -> {
                    ps.setLong(1, chatRoomMessage.getChatRoomId());
                    ps.setString(2, chatRoomMessage.getChatMessageId());
                }
            );
    }

    @Override
    public void bulkChatMessage(List<ChatMessageBulk> chatMessageBulks) {
        String sql = SqlGenerator.bulkChatMessageSql();
        jdbcTemplate.batchUpdate(sql, chatMessageBulks, chatMessageBulks.size(),
                (ps, chatMessage) -> {
                    ps.setString(1, chatMessage.getId());
                    ps.setString(2,chatMessage.getContent());
                    ps.setLong(3,chatMessage.getPublishedById());
                    ps.setTimestamp(4,Timestamp.valueOf(chatMessage.getPublishedAt()));
                }
            );

    }

    @Override
    public Optional<ChatRoom> fetchChatRoomByUserIdAndChatRoomId(Long userId, Long chatRoomId) {
        return chatRoomJpaRepository.findByUserAndChatRoom(userId, chatRoomId);
    }@Override
    public Optional<ChatMessage> joinFetchChatMessage(Long chatRoomId) {
        return chatMessageJpaRepository.joinFetchChatMessage(chatRoomId);
    }

    @Override
    public Page<ChatMessageElement> fetch(Pageable pageable, Long chatRoomId) {
        return chatMessageJpaRepository.fetch(chatRoomId, pageable);
    }

    @Override
    public Optional<ChatRoom> fetchChatRoom(Long chatRoomId) {
        return chatRoomJpaRepository.findById(chatRoomId);
    }

    @Override
    public Optional<ChatParticipant> fetchChatParticipant(Long chatRoomId, Long userId) {
        return chatRoomJpaRepository.findByUserAndChatRoom(userId, chatRoomId)
                .map(chatRoom -> resolveParticipant(chatRoom, userId));
    }

    @Override
    public Optional<ChatMessage> fetchChatMessage(String chatMessageId) {
        return chatMessageJpaRepository.findById(chatMessageId);
    }

    private ChatParticipant resolveParticipant(ChatRoom chatRoom, Long userId) {
        ChatParticipant creator = chatRoom.getCreator();
        return creator.getUser().getId().equals(userId) ? creator : chatRoom.getOpponent();
    }
}
