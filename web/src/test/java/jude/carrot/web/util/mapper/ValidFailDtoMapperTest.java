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

    private static final String EMAIL_FIELD = "email";
    private static final String INVALID_FIELD_NAME = "password";

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
    @SuppressWarnings("java:S2068") // "password"는 검증 대상 필드명 상수일 뿐 실제 비밀번호 값이 아님 (오탐, 상수 추출로도 회피 안 됨을 확인함)
    void fromMapsFieldErrorsToValidFailDto() {
        BindingResult bindingResult = bindingResultOf(
                fieldError(EMAIL_FIELD, "이메일을 입력해주세요."),
                fieldError(INVALID_FIELD_NAME, "비밀번호를 입력해주세요.")
        );

        List<ValidFailDto> result = ValidFailDtoMapper.from(bindingResult);

        assertThat(result)
                .extracting(ValidFailDto::getField, ValidFailDto::getMessage)
                .containsExactlyInAnyOrder(
                        tuple(EMAIL_FIELD, "이메일을 입력해주세요."),
                        tuple(INVALID_FIELD_NAME, "비밀번호를 입력해주세요.")
                );
    }

    @Test
    @DisplayName("필드 에러가 없으면 빈 목록을 반환한다")
    void fromReturnsEmptyListWhenNoErrors() {
        BindingResult bindingResult = bindingResultOf();

        List<ValidFailDto> result = ValidFailDtoMapper.from(bindingResult);

        assertThat(result).isEmpty();
    }
}
