package jude.carrot.infra.repository.image.jpa;

import jude.carrot.infra.entity.image.ThumbnailImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThumbnailImageJpaRepository extends JpaRepository<ThumbnailImage,Long> {
}
