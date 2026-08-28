package jude.carrot.chatserver.response;

import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import lombok.Builder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@Builder
public record ChatMessageResponse(
        List<ChatMessageElement> page,
        int pageSize,
        int pageNum,
        int totalPage,
        int elementCount,
        boolean isLast
) {

    public ChatMessageResponse {
        if (page == null) {
            page = new ArrayList<>();
        }
    }

    public static ChatMessageResponse from(Page<ChatMessageElement> page) {
        Pageable pageable = page.getPageable();
        List<ChatMessageElement> content = page.getContent();
        return ChatMessageResponse.builder()
                .page(content)
                .pageSize(pageable.getPageSize())
                .pageNum(pageable.getPageNumber())
                .elementCount(content.size())
                .totalPage(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }
}
