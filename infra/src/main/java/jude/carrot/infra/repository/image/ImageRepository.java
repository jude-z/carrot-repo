package jude.carrot.infra.repository.image;


import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.image.SingleImage;

import java.util.List;

public interface ImageRepository {
    SingleImage save(SingleImage image);
    void saveAll(List<MultipleImage> multipleImages);
}
