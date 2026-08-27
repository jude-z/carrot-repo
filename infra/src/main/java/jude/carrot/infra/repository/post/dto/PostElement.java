package jude.carrot.infra.repository.post.dto;

import jude.carrot.infra.entity.user.Address;
import lombok.Builder;

@Builder
public record PostElement(
        Long id,
        String title,
        Integer price,
        Address address,
        Long createdById,
        String createdByEmail,
        String thumbnailImageUrl
) {
}
