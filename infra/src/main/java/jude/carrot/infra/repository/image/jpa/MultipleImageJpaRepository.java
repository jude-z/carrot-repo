package jude.carrot.infra.repository.image.jpa;

import jude.carrot.infra.entity.image.MultipleImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MultipleImageJpaRepository extends JpaRepository<MultipleImage,Long> {
}
