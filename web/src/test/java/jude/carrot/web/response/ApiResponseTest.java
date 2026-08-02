package jude.carrot.web.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static jude.carrot.service.status.Status.SUCCESS;
import static jude.carrot.service.status.Status.VALID_FAIL;
import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("successFrom(data)는 SUCCESS 코드와 데이터를 담는다")
    void successFrom_withData() {
        ApiResponse<String> response = ApiResponse.successFrom("hello");

        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getCode()).isEqualTo(SUCCESS.getCode());
        assertThat(response.getDetailMessage()).isEqualTo(SUCCESS.getDetailMessage());
    }

    @Test
    @DisplayName("successFrom()은 데이터 없이 SUCCESS 코드만 담는다")
    void successFrom_withoutData() {
        ApiResponse<Void> response = ApiResponse.successFrom();

        assertThat(response.getData()).isNull();
        assertThat(response.getCode()).isEqualTo(SUCCESS.getCode());
        assertThat(response.getDetailMessage()).isEqualTo(SUCCESS.getDetailMessage());
    }

    @Test
    @DisplayName("failFrom(data, code, message)은 지정한 코드/메시지/데이터를 담는다")
    void failFrom_withData() {
        ApiResponse<String> response =
                ApiResponse.failFrom("field error", VALID_FAIL.getCode(), VALID_FAIL.getDetailMessage());

        assertThat(response.getData()).isEqualTo("field error");
        assertThat(response.getCode()).isEqualTo(VALID_FAIL.getCode());
        assertThat(response.getDetailMessage()).isEqualTo(VALID_FAIL.getDetailMessage());
    }

    @Test
    @DisplayName("failFrom(code, message)은 데이터 없이 코드/메시지만 담는다")
    void failFrom_withoutData() {
        ApiResponse<Void> response = ApiResponse.failFrom(VALID_FAIL.getCode(), VALID_FAIL.getDetailMessage());

        assertThat(response.getData()).isNull();
        assertThat(response.getCode()).isEqualTo(VALID_FAIL.getCode());
        assertThat(response.getDetailMessage()).isEqualTo(VALID_FAIL.getDetailMessage());
    }
}
