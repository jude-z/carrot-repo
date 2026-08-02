package jude.carrot.infra.fixture.post;

import jude.carrot.infra.entity.image.PostImage;
import jude.carrot.infra.entity.image.ThumbnailImage;
import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;

import java.util.ArrayList;
import java.util.List;

public class PostFactory {

    private PostFactory() {
    }

    public static Post create(User user) {
        return Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .contentImages(new ArrayList<>())
                .build();
    }

    public static Post createWithImages(User user, String thumbnailImageUrl, List<String> contentImageUrls) {
        Post post = Post.builder()
                .title("title")
                .content("content")
                .price(10000)
                .address(Address.from("서울시", "강남구", "역삼동"))
                .user(user)
                .thumbnailImage(ThumbnailImage.from(thumbnailImageUrl))
                .contentImages(new ArrayList<>())
                .build();
        List<PostImage> contentImages = contentImageUrls.stream()
                .map(url -> PostImage.from(url, post))
                .toList();
        post.setContentImages(contentImages);
        return post;
    }
}
