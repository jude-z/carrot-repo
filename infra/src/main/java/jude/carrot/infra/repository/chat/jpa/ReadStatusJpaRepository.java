package jude.carrot.infra.repository.chat.jpa;


import jude.carrot.infra.entity.chat.ReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadStatusJpaRepository extends JpaRepository<ReadStatus,Long> {

}
