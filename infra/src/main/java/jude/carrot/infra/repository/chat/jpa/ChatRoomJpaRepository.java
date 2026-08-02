package jude.carrot.infra.repository.chat.jpa;


import jude.carrot.infra.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, Long> {

    @Query("select c from ChatRoom c " +
            "where c.id = :chatRoomId " +
            "and (c.creator.user.id = :userId or c.opponent.user.id = :userId)")
    Optional<ChatRoom> findByUserAndChatRoom(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);
}
