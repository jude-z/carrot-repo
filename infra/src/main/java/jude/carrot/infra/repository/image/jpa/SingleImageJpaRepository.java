package jude.carrot.infra.repository.image.jpa;

import jude.carrot.infra.entity.image.SingleImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SingleImageJpaRepository extends JpaRepository<SingleImage,Long> {
}
