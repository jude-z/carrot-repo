package jude.carrot.infra.entity.post;

import jakarta.persistence.*;
import jude.carrot.infra.entity.image.PostImage;
import jude.carrot.infra.entity.image.ThumbnailImage;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    private Integer price;
    private Address address;
    @ManyToOne
    private User createdBy;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private ThumbnailImage thumbnailImage;
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostImage> contentImages = new ArrayList<>();

    @Builder
    private Post(String title, String content, Integer price, Address address, User user, ThumbnailImage thumbnailImage, List<PostImage> contentImages){
        this.title = title;
        this.content = content;
        this.price = price;
        this.address = address;
        this.createdBy = user;
        this.thumbnailImage = thumbnailImage;
        this.contentImages = contentImages;
    }
}
