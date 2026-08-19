package jude.carrot.infra.repository.chat.jpa;


import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.repository.chat.dto.ChatDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, String> {
    @Query("select c from ChatMessage c " +
            "left join fetch c.chatRoom " +
            "left join fetch c.publishedBy " +
            "where c.chatRoom.id = :chatRoomId " +
            "order by c.id desc " +
            "limit 1")
    Optional<ChatMessage> joinFetchChatMessage(@Param("chatRoomId") Long chatRoomId);

    @Query(value = "select new jude.carrot.infra.repository.chat.dto.ChatDto$ChatMessageElement(" +
            "c.id, c.content, c.publishedBy.id, c.publishedAt) " +
            "from ChatMessage c " +
            "where c.chatRoom.id = :chatRoomId " +
            "order by c.id desc",
            countQuery = "select count(c) from ChatMessage c where c.chatRoom.id = :chatRoomId")
    Page<ChatDto.ChatMessageElement> fetch(@Param("chatRoomId") Long chatRoomId, Pageable pageable);
}
