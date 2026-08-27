package jude.carrot.infra.repository.image;

import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.repository.image.jpa.MultipleImageJpaRepository;
import jude.carrot.infra.repository.image.jpa.SingleImageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ImageRepositoryImpl implements ImageRepository {
    private final SingleImageJpaRepository singleImageJpaRepository;

    private final MultipleImageJpaRepository multipleImageJpaRepository;
    @Override
    public SingleImage save(SingleImage image) {
        return singleImageJpaRepository.save(image);
    }

    @Override
    public void saveAll(List<MultipleImage> multipleImages) {
        multipleImageJpaRepository.saveAll(multipleImages);
    }
}
