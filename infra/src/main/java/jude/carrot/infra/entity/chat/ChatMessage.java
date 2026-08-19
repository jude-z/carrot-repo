package jude.carrot.infra.entity.chat;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    private String id;
    private String content;
    @ManyToOne
    private ChatRoom chatRoom;
    @ManyToOne
    private ChatParticipant publishedBy;
    private LocalDateTime publishedAt;
}
