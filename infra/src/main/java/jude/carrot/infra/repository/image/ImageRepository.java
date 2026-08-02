package jude.carrot.infra.repository.image;


import jude.carrot.infra.entity.image.ThumbnailImage;

public interface ImageRepository {
    ThumbnailImage save(ThumbnailImage image);
}
