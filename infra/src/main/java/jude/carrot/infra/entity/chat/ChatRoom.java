package jude.carrot.infra.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @ManyToOne
    private ChatParticipant creator;
    @ManyToOne
    private ChatParticipant opponent;
    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

    public static ChatRoom from(String title, ChatParticipant creator, ChatParticipant opponent){
        return ChatRoom.builder()
                .title(title)
                .creator(creator)
                .opponent(opponent)
                .build();
    }
}
