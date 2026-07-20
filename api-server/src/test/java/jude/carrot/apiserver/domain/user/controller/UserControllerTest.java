package jude.carrot.apiserver.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jude.carrot.apiserver.domain.user.dto.request.UserRequest;
import jude.carrot.apiserver.domain.user.exception.UserFoundException;
import jude.carrot.apiserver.common.status.Status;
import jude.carrot.apiserver.domain.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static jude.carrot.apiserver.domain.user.fixture.dto.request.RequestFactory.*;
import static jude.carrot.apiserver.domain.user.fixture.urls.TestUrl.*;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "abcd123!";

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    UserService userService;

    private ResultActions performSignUp(String email, String password, String confirmPassword) throws Exception {
        UserRequest.SignUpRequest signUpRequest = createSignUpRequest(email, password, confirmPassword);

        return mockMvc.perform(
                post(SIGN_UP.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest))
        );
    }

    @Test
    @DisplayName("모든 필드가 유효하면 회원가입에 성공하고 200을 반환한다")
    void signUp_success() throws Exception {
        performSignUp(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(Status.SUCCESS.getCode()))
                .andExpect(jsonPath("$.detailMessage").value(Status.SUCCESS.getDetailMessage()));

        verify(userService, times(1))
                .signUp(createSignUpRequest(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD));
    }

    @Test
    @DisplayName("이메일이 비어있으면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_emailBlank() throws Exception {
        performSignUp("", VALID_PASSWORD, VALID_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(Status.VALID_FAIL.getCode()))
                .andExpect(jsonPath("$.data[?(@.field == 'email')].message")
                        .value(hasItem("이메일을 입력해주세요.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("이메일 형식이 올바르지 않으면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_emailInvalidFormat() throws Exception {
        performSignUp("invalid-email-format", VALID_PASSWORD, VALID_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'email')].message")
                        .value(hasItem("이메일 형식이 올바르지 않습니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호가 비어있으면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_passwordBlank() throws Exception {
        performSignUp(VALID_EMAIL, "", VALID_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'password')].message")
                        .value(hasItem("비밀번호를 입력해주세요.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호 길이가 8자 미만이면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_passwordTooShort() throws Exception {
        performSignUp(VALID_EMAIL, "a1!", "a1!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'password')].message")
                        .value(hasItem("비밀번호는 8자 이상이어야 합니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호가 영문/숫자/특수문자 조합이 아니면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_passwordPatternMismatch() throws Exception {
        performSignUp(VALID_EMAIL, "abcdefgh", "abcdefgh")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'password')].message")
                        .value(hasItem("비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호 확인이 비어있으면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_confirmPasswordBlank() throws Exception {
        performSignUp(VALID_EMAIL, VALID_PASSWORD, "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'confirmPassword')].message")
                        .value(hasItem("비밀번호를 입력해주세요.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호 확인 길이가 8자 미만이면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_confirmPasswordTooShort() throws Exception {
        performSignUp(VALID_EMAIL, VALID_PASSWORD, "a1!")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'confirmPassword')].message")
                        .value(hasItem("비밀번호는 8자 이상이어야 합니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("비밀번호 확인이 영문/숫자/특수문자 조합이 아니면 400과 검증 실패 메시지를 반환한다")
    void signUp_fail_confirmPasswordPatternMismatch() throws Exception {
        performSignUp(VALID_EMAIL, VALID_PASSWORD, "abcdefgh")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'confirmPassword')].message")
                        .value(hasItem("비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("모든 필드가 비어있으면 각 필드별 검증 실패 메시지가 모두 반환된다")
    void signUp_fail_allFieldsBlank() throws Exception {
        performSignUp("", "", "")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data[?(@.field == 'email')].message")
                        .value(hasItem("이메일을 입력해주세요.")))
                .andExpect(jsonPath("$.data[?(@.field == 'password')].message")
                        .value(hasItem("비밀번호를 입력해주세요.")))
                .andExpect(jsonPath("$.data[?(@.field == 'confirmPassword')].message")
                        .value(hasItem("비밀번호를 입력해주세요.")));

        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409를 반환한다")
    void signUp_fail_userAlreadyExists() throws Exception {
        willThrow(new UserFoundException(Status.USER_EXIST))
                .given(userService).signUp(any());

        performSignUp(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(Status.USER_EXIST.getCode()))
                .andExpect(jsonPath("$.detailMessage").value(Status.USER_EXIST.getDetailMessage()));

        verify(userService, times(1))
                .signUp(createSignUpRequest(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD));
    }
}
