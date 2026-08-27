package jude.carrot.apiserver.domain.post.response;


import jude.carrot.infra.entity.image.MultipleImage;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.post.Post;
import jude.carrot.infra.entity.user.Address;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

import jude.carrot.infra.repository.post.dto.PostElement;

public class PostResponse {
    private PostResponse(){}

    @Builder
    public record FetchPostResponse(
            Long id,
            String title,
            Integer price,
            Address address,
            Long createdById,
            String createdByEmail,
            String thumbnailImageUrl,
            List<String> contentImageUrls
    ) {

        public FetchPostResponse {
            if (contentImageUrls == null) contentImageUrls = new ArrayList<>();
        }

        public static FetchPostResponse from(Post post) {
            SingleImage thumbnailImage = post.getThumbnailImage();
            String thumbnailImageUrl = thumbnailImage != null ? thumbnailImage.getUrl() : null;
            List<MultipleImage> contentImages = post.getContentImages();
            List<String> contentImageUrls = contentImages.stream()
                    .map(MultipleImage::getUrl)
                    .toList();
            return FetchPostResponse.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .price(post.getPrice())
                    .address(post.getAddress())
                    .createdByEmail(post.getCreatedBy().getEmail())
                    .createdById(post.getCreatedBy().getId())
                    .thumbnailImageUrl(thumbnailImageUrl)
                    .contentImageUrls(contentImageUrls)
                    .build();
        }

    }

    @Builder
    public record FetchPostsResponse(
            List<PostElement> page,
            int pageSize,
            int pageNum,
            int totalPage,
            int elementCount,
            boolean isLast
    ) {

        public FetchPostsResponse {
            if (page == null) page = new ArrayList<>();
        }

        public static FetchPostsResponse from(Page<PostElement> page){
            Pageable pageable = page.getPageable();
            List<PostElement> content = page.getContent();
            return FetchPostsResponse.builder()
                    .page(content)
                    .pageSize(pageable.getPageSize())
                    .pageNum(pageable.getPageNumber())
                    .elementCount(content.size())
                    .totalPage(page.getTotalPages())
                    .isLast(page.isLast())
                    .build();
        }
    }

    @Builder
    public record CreatePostResponse(
            Long id
    ) {

        public static CreatePostResponse from(Long id){
            return CreatePostResponse.builder()
                    .id(id)
                    .build();
        }
    }

    @Builder
    public record UpdatePostResponse(
            Long id
    ) {

        public static UpdatePostResponse from(Long id){
            return UpdatePostResponse.builder()
                    .id(id)
                    .build();
        }

    }

}
