package jude.carrot.web.util.mapper;

import jude.carrot.web.dto.ValidFailDto;
import jude.carrot.web.mapper.ValidFailDtoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ValidFailDtoMapperTest {

    private BindingResult bindingResultOf(FieldError... errors) {
        BindingResult bindingResult = new MapBindingResult(Map.of(), "loginRequest");
        for (FieldError error : errors) {
            bindingResult.addError(error);
        }
        return bindingResult;
    }

    private FieldError fieldError(String field, String message) {
        return new FieldError("loginRequest", field, null, false, null, null, message);
    }

    @Test
    @DisplayName("BindingResult의 필드 에러들을 ValidFailDto 목록으로 변환한다")
    void from_mapsFieldErrorsToValidFailDto() {
        BindingResult bindingResult = bindingResultOf(
                fieldError("email", "이메일을 입력해주세요."),
                fieldError("password", "비밀번호를 입력해주세요.")
        );

        List<ValidFailDto> result = ValidFailDtoMapper.from(bindingResult);

        assertThat(result)
                .extracting(ValidFailDto::getField, ValidFailDto::getMessage)
                .containsExactlyInAnyOrder(
                        tuple("email", "이메일을 입력해주세요."),
                        tuple("password", "비밀번호를 입력해주세요.")
                );
    }

    @Test
    @DisplayName("필드 에러가 없으면 빈 목록을 반환한다")
    void from_returnsEmptyList_whenNoErrors() {
        BindingResult bindingResult = bindingResultOf();

        List<ValidFailDto> result = ValidFailDtoMapper.from(bindingResult);

        assertThat(result).isEmpty();
    }
}
