package jude.carrot.apiserver.domain.user.controller;

import jude.carrot.apiserver.domain.user.fixture.url.TestUrl;
import jude.carrot.apiserver.domain.user.service.UserService;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.advice.CommonControllerAdvice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static jude.carrot.service.status.Status.PASSWORD_CONFIRM_NOT_MATCH;
import static jude.carrot.service.status.Status.USER_EXIST;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController의 요청 바인딩/검증(@Valid)이 CommonControllerAdvice를 거쳐
 * VALID_FAIL 응답으로, CustomException이 실패 응답으로 이어지는지 확인하는 MVC 레이어 테스트.
 */
@WebMvcTest(controllers = AuthController.class)
@Import(CommonControllerAdvice.class)
class AuthControllerTest {

    private static final String VALID_EMAIL = "carrot@carrot.com";
    private static final String VALID_PASSWORD = "password1!";
    private static final String VALID_NICKNAME = "carrot";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private String signUpJson(String email, String password, String confirmPassword, String nickname) {
        String emailJson = email == null ? "null" : "\"" + email + "\"";
        String passwordJson = password == null ? "null" : "\"" + password + "\"";
        String confirmPasswordJson = confirmPassword == null ? "null" : "\"" + confirmPassword + "\"";
        String nicknameJson = nickname == null ? "null" : "\"" + nickname + "\"";
        return """
                {"email":%s,"password":%s,"confirmPassword":%s,"nickname":%s}
                """.formatted(emailJson, passwordJson, confirmPasswordJson, nicknameJson);
    }

    @Test
    @DisplayName("올바른 값으로 회원가입을 요청하면 200을 반환하고 서비스를 호출한다")
    void signUp_success() throws Exception {
        mockMvc.perform(post(TestUrl.SIGN_UP.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD, VALID_NICKNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"));

        verify(userService).signUp(any());
    }

    @Test
    @DisplayName("이미 가입된 이메일로 회원가입을 요청하면 409와 함께 USER_EXIST 코드를 반환한다")
    void signUp_fail_whenEmailAlreadyExists() throws Exception {
        doThrow(new CustomException(USER_EXIST)).when(userService).signUp(any());

        mockMvc.perform(post(TestUrl.SIGN_UP.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD, VALID_NICKNAME)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(USER_EXIST.getCode()));
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 다르면 400과 함께 PASSWORD_CONFIRM_NOT_MATCH 코드를 반환한다")
    void signUp_fail_whenPasswordConfirmMismatch() throws Exception {
        doThrow(new CustomException(PASSWORD_CONFIRM_NOT_MATCH)).when(userService).signUp(any());

        mockMvc.perform(post(TestUrl.SIGN_UP.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(VALID_EMAIL, VALID_PASSWORD, "otherPassword1!", VALID_NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(PASSWORD_CONFIRM_NOT_MATCH.getCode()));
    }

    @ParameterizedTest(name = "email=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("이메일이 비어있거나 형식이 잘못되면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    @ValueSource(strings = {"not-an-email"})
    void signUp_fail_whenEmailInvalid(String invalidEmail) throws Exception {
        mockMvc.perform(post(TestUrl.SIGN_UP.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(invalidEmail, VALID_PASSWORD, VALID_PASSWORD, VALID_NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("email")));

        verify(userService, never()).signUp(any());
    }

    @ParameterizedTest(name = "password=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("비밀번호가 비어있거나 형식이 잘못되면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    @ValueSource(strings = {"short1!", "password", "12345678"})
    void signUp_fail_whenPasswordInvalid(String invalidPassword) throws Exception {
        mockMvc.perform(post(TestUrl.SIGN_UP.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(VALID_EMAIL, invalidPassword, invalidPassword, VALID_NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("password")));

        verify(userService, never()).signUp(any());
    }

    @ParameterizedTest(name = "nickname=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("닉네임이 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void signUp_fail_whenNicknameBlank(String invalidNickname) throws Exception {
        mockMvc.perform(post(TestUrl.SIGN_UP.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signUpJson(VALID_EMAIL, VALID_PASSWORD, VALID_PASSWORD, invalidNickname)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("nickname")));

        verify(userService, never()).signUp(any());
    }
}
