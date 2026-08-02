package jude.carrot.infra.entity.chat;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    private ChatParticipant chatParticipant;
    @ManyToOne
    private ChatMessage chatMessage;
    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

    @Builder
    private ReadStatus(ChatParticipant chatParticipant, ChatMessage chatMessage){
        this.chatParticipant = chatParticipant;
        this.chatMessage = chatMessage;
    }

    public static ReadStatus from(ChatParticipant chatParticipant, ChatMessage chatMessage){
        return ReadStatus.builder()
                .chatParticipant(chatParticipant)
                .chatMessage(chatMessage)
                .build();
    }
}
