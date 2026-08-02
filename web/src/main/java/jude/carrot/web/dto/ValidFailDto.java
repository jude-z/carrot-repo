package jude.carrot.web.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValidFailDto {

    private String field;
    private String message;

    public static ValidFailDto from(String field, String message){
        return ValidFailDto.builder()
                .field(field)
                .message(message)
                .build();
    }

}
