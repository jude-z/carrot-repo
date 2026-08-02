package jude.carrot.infra.entity.user;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class Address {

    private String sido;
    private String gu;
    private String dong;

    @Builder
    private Address(String sido, String gu, String dong) {
        this.sido = sido;
        this.gu = gu;
        this.dong = dong;
    }

    public static Address from(String sido, String gu, String dong){
        return Address.builder()
                .sido(sido)
                .gu(gu)
                .dong(dong)
                .build();
    }
}
