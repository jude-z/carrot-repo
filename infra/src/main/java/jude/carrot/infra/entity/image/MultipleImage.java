package jude.carrot.infra.entity.image;

import jakarta.persistence.*;
import jude.carrot.infra.entity.post.Post;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MultipleImage{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;
    protected String url;

    @ManyToOne
    private Post post;

    @Builder
    private MultipleImage(String url, Post post){
        this.url = url;
        this.post = post;
    }

    public static MultipleImage from(String url, Post post){
        return MultipleImage.builder()
                .url(url)
                .post(post)
                .build();
    }

    public static MultipleImage from(String url){
        return MultipleImage.builder()
                .url(url)
                .build();
    }

}
