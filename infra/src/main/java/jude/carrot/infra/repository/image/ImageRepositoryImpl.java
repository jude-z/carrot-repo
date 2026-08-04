package jude.carrot.infra.repository.image;

import jude.carrot.infra.entity.image.ThumbnailImage;
import jude.carrot.infra.repository.image.jpa.ThumbnailImageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepository {

    private final ThumbnailImageJpaRepository thumbnailImageJpaRepository;

    @Override
    public ThumbnailImage save(ThumbnailImage image) {
        return thumbnailImageJpaRepository.save(image);
    }
}
