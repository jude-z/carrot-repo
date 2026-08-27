package jude.carrot.web.dto;

import lombok.Builder;

@Builder
public record ValidFailDto(
        String field,
        String message
) {

    public static ValidFailDto from(String field, String message){
        return ValidFailDto.builder()
                .field(field)
                .message(message)
                .build();
    }
}
