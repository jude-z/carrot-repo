package jude.carrot.infra.repository.chat.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReadStatusBulk(
        Long chatParticipantId,
        String chatMessageId,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    public static ReadStatusBulk from(Long chatParticipantId, String chatMessageId) {
        return ReadStatusBulk.builder()
                .chatParticipantId(chatParticipantId)
                .chatMessageId(chatMessageId)
                .build();
    }
}
