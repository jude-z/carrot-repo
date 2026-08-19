package jude.carrot.infra.entity.recover;

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
public class RedisRecover {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String key;
    private String value;
    @Enumerated(EnumType.STRING)
    private DataType dataType;
    private Double score;
    private boolean status;
    @CreatedDate
    private LocalDateTime createTime;
    @LastModifiedDate
    private LocalDateTime updateTime;

    public static RedisRecover from(String key, String value, DataType dataType, Double score){
        return RedisRecover.builder()
                .key(key)
                .value(value)
                .dataType(dataType)
                .score(score)
                .status(true)
                .build();
    }

}
