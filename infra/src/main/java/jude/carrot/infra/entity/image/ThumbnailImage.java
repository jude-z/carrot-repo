package jude.carrot.infra.entity.image;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ThumbnailImage extends BaseImage {

    @Builder
    private ThumbnailImage(String url){
        this.url = url;
    }

    public static ThumbnailImage from(String url){
        if(!StringUtils.hasText(url)) return null;
        return ThumbnailImage.builder()
                .url(url)
                .build();
    }
}
