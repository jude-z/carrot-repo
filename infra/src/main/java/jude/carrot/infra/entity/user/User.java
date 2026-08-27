package jude.carrot.infra.entity.user;

import jakarta.persistence.*;
import jude.carrot.infra.entity.image.SingleImage;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String email;
    private String password;
    private String nickname;
    @OneToOne
    private SingleImage image;
    @Embedded
    private Address address;
    private boolean verified;
    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

    @Builder
    private User(String email, String password, String nickname, SingleImage image, Address address
            , LocalDateTime lastActiveTime, boolean verified){
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.image = image;
        this.address = address;
        this.verified = verified;
    }

    public static User from(String email, String password, String nickname, SingleImage image){
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .image(image)
                .build();
    }
}
