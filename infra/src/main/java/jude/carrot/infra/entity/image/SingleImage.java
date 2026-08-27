package jude.carrot.infra.entity.image;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.util.StringUtils;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SingleImage{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    protected String url;

    @Builder
    private SingleImage(String url){
        this.url = url;
    }

    public static SingleImage from(String url){
        if(!StringUtils.hasText(url)) return null;
        return SingleImage.builder()
                .url(url)
                .build();
    }
}
