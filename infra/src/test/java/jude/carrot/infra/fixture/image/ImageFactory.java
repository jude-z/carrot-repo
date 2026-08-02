package jude.carrot.infra.fixture.image;

import jude.carrot.infra.entity.image.ThumbnailImage;

public class ImageFactory {
    public static ThumbnailImage create(String url){
        return ThumbnailImage.builder()
                .url(url)
                .build();
    }
}
