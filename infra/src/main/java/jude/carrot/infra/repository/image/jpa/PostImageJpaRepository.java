package jude.carrot.infra.repository.image.jpa;

import jude.carrot.infra.entity.image.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageJpaRepository extends JpaRepository<PostImage,Long> {
}
