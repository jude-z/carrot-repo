package jude.carrot.infra.repository.chat.dto;

import jakarta.persistence.ManyToOne;
import jude.carrot.infra.entity.chat.ChatMessage;
import jude.carrot.infra.entity.chat.ChatParticipant;
import jude.carrot.infra.entity.chat.ChatRoom;
import lombok.*;

import java.time.LocalDateTime;

public class ChatDto {
    private ChatDto(){}


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadStatusBulk{
        private Long chatParticipantId;
        private String chatMessageId;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        public static ReadStatusBulk from(Long chatParticipantId, String chatMessageId) {
            return ReadStatusBulk.builder()
                    .chatParticipantId(chatParticipantId)
                    .chatMessageId(chatMessageId)
                    .build();
        }
    }
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRoomMessageBulk{
        private Long chatRoomId;
        private String chatMessageId;
        public static ChatRoomMessageBulk from(Long chatRoomId, RedisChatRoomMessage redisChatRoomMessage){
            return ChatRoomMessageBulk.builder()
                    .chatRoomId(chatRoomId)
                    .chatMessageId(redisChatRoomMessage.getChatMessageId())
                    .build();
        }
    }
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageBulk{
        private String id;
        private String content;
        private Long publishedById;
        private LocalDateTime publishedAt;

        public static ChatMessageBulk from(String id, RedisChatMessage redisChatMessage){
            return ChatMessageBulk.builder()
                    .id(id)
                    .content(redisChatMessage.getContent())
                    .publishedById(redisChatMessage.getPublishedBy())
                    .publishedAt(redisChatMessage.getPublishedAt())
                    .build();
        }
    }

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageElement{
        private String id;
        private String content;
        private Long publishedBy;
        private LocalDateTime publishedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChatRoomRequest {
        private Long opponentId;
        private String title;
    }
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FetchRecentChatMessageRequest{
        String content;
    }


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishChatRequest{
        private String content;
    }
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisChatMessage {
        private String id;
        private String content;
        private Long publishedBy;
        private LocalDateTime publishedAt;

        public static RedisChatMessage from(String id, PublishChatRequest publishChatRequest, LocalDateTime publishedAt, Long publishedBy){

            return RedisChatMessage.builder()
                    .id(id)
                    .content(publishChatRequest.getContent())
                    .publishedBy(publishedBy)
                    .publishedAt(publishedAt)
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisChatRoomMessage{
        private String chatMessageId;

        public static RedisChatRoomMessage from(String chatMessageId){
            return RedisChatRoomMessage.builder()
                    .chatMessageId(chatMessageId)
                    .build();
        }
    }


    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RedisReadStatus{
        private String chatMessageId;

        public static RedisReadStatus from(String chatMessageId){
            return RedisReadStatus.builder()
                    .chatMessageId(chatMessageId)
                    .build();
        }
    }

}
