package jude.carrot.chatserver.response;

import jude.carrot.infra.repository.chat.dto.ChatMessageElement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public record ChatMessageResponse(
        List<ChatMessageElement> page,
        int pageSize,
        int pageNum,
        int totalPage,
        int elementCount,
        boolean isLast
) {

    public ChatMessageResponse {
        page = page == null ? List.of() : List.copyOf(page);
        elementCount = page.size();
    }

    public ChatMessageResponse(List<ChatMessageElement> page, int pageSize, int pageNum, int totalPage, boolean isLast) {
        this(page, pageSize, pageNum, totalPage, 0, isLast);
    }

    public static ChatMessageResponse from(Page<ChatMessageElement> page) {
        Pageable pageable = page.getPageable();
        return new ChatMessageResponse(
                page.getContent(),
                pageable.getPageSize(),
                pageable.getPageNumber(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
