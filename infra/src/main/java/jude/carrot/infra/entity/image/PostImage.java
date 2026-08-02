package jude.carrot.infra.entity.image;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jude.carrot.infra.entity.post.Post;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostImage extends BaseImage{

    @ManyToOne
    private Post post;

    @Builder
    private PostImage(String url,Post post){
        this.url = url;
        this.post = post;
    }

    public static PostImage from(String url,Post post){
        return PostImage.builder()
                .url(url)
                .post(post)
                .build();
    }

    public static PostImage from(String url){
        return PostImage.builder()
                .url(url)
                .build();
    }

}
