package jude.carrot.apiserver.domain.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String email;
    private String password;
    @Column(name = "access_token")
    @Setter
    private String accessToken;
    @Column(name = "refresh_token")
    @Setter
    private String refreshToken;

    @Builder
    private User(String email,String password){
        this.email = email;
        this.password = password;
    }

}
