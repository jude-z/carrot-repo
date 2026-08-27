package jude.carrot.web.mapper;


import jude.carrot.web.dto.ValidFailDto;
import org.springframework.validation.BindingResult;

import java.util.List;

public class ValidFailDtoMapper {

    private ValidFailDtoMapper() {
    }

    public static List<ValidFailDto> from(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> ValidFailDto.from(error.getField(), error.getDefaultMessage()))
                .toList();
    }
}
