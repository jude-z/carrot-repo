package jude.carrot.infra.repository.chat.jpa;


import jude.carrot.infra.entity.chat.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {
    @Query("select c from ChatMessage c " +
            "left join fetch c.chatRoom " +
            "left join fetch c.publishedBy " +
            "where c.chatRoom.id = :chatRoomId " +
            "order by c.id desc " +
            "limit 1")
    Optional<ChatMessage> joinFetchChatMessage(@Param("chatRoomId") Long chatRoomId);
}
