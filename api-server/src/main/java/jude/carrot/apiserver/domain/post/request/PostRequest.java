package jude.carrot.apiserver.domain.post.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import lombok.Builder;

import java.util.List;

public class PostRequest {

    private PostRequest(){}

    @Builder
    public record PostCreateRequest(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,

            @NotBlank(message = "내용을 입력해주세요.")
            String content,

            @NotNull(message = "가격을 입력해주세요.")
            @Positive(message = "가격은 0보다 커야 합니다.")
            Integer price,

            @NotBlank(message = "위도를 입력해주세요.")
            String latitude,

            @NotBlank(message = "경도를 입력해주세요.")
            String longitude,

            String thumbNailImageUrl,

            @NotEmpty(message = "게시글 이미지를 1개 이상 등록해주세요.")
            List<String> contentImageUrls
    ) {
    }

    @Builder
    public record PostUpdateRequest(
            @NotBlank(message = "제목을 입력해주세요.")
            String title,

            @NotBlank(message = "내용을 입력해주세요.")
            String content,

            @NotNull(message = "가격을 입력해주세요.")
            @Positive(message = "가격은 0보다 커야 합니다.")
            Integer price,

            String thumbNailImageUrl,

            @NotEmpty(message = "게시글 이미지를 1개 이상 등록해주세요.")
            List<String> contentImageUrls
    ) {
    }

    public static Post from(PostCreateRequest postCreateRequest, Address address, User user){
        String thumbnailImageUrl = postCreateRequest.thumbNailImageUrl();
        List<String> contentImageUrls = postCreateRequest.contentImageUrls();
        List<MultipleImage> contentImages = contentImageUrls.stream()
                .map(MultipleImage::from)
                .toList();
        Post post = Post.builder()
                        .title(postCreateRequest.title())
                        .price(postCreateRequest.price())
                        .address(address)
                        .user(user)
                        .thumbnailImage(SingleImage.from(thumbnailImageUrl))
                        .contentImages(contentImages)
                        .build();
        contentImages.forEach(image -> image.setPost(post));
        return post;
    }

}
