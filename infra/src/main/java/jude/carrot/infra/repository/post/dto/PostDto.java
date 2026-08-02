package jude.carrot.infra.repository.post.dto;

import jude.carrot.infra.entity.user.Address;
import lombok.*;

public class PostDto {
    private PostDto(){}
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostElement{
        private Long id;
        private String title;
        private Integer price;
        private Address address;
        private Long createdById;
        private String createdByEmail;
        private String thumbnailImageUrl;
    }
}
