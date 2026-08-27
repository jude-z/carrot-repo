package jude.carrot.infra.repository.chat.jpa;


import jude.carrot.infra.entity.chat.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantJpaRepository extends JpaRepository<ChatParticipant,Long> {
}
