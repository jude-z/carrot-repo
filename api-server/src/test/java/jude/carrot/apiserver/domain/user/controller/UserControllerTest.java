package jude.carrot.apiserver.domain.user.controller;

import jude.carrot.apiserver.domain.user.service.UserService;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.advice.CommonControllerAdvice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static jude.carrot.apiserver.domain.user.response.UserResponse.*;
import static jude.carrot.service.status.Status.PASSWORD_CONFIRM_NOT_MATCH;
import static jude.carrot.service.status.Status.UPDATE_USER_NOT_AUTHORIZE;
import static jude.carrot.service.status.Status.USER_NOT_EXIST;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.isIn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController의 요청 바인딩/검증(@Valid)이 CommonControllerAdvice를 거쳐 VALID_FAIL 응답으로,
 * UserService가 던지는 CustomException이 실패 응답으로 이어지는지 확인하는 MVC 레이어 테스트.
 * {@code @AuthenticationPrincipal}는 컨트롤러 시그니처상 반드시 있어야 해서 최소한으로만 채워준다.
 */
@WebMvcTest(controllers = UserController.class)
@Import({CommonControllerAdvice.class, UserControllerTest.ArgumentResolverConfig.class})
class UserControllerTest {

    private static final Long REQUESTER_ID = 1L;
    private static final String VALID_PASSWORD = "password1!";
    private static final String VALID_NICKNAME = "carrot";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUpPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(REQUESTER_ID, null));
    }

    @AfterEach
    void clearPrincipal() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("사용자를 조회하면 200과 함께 데이터를 반환한다")
    void fetchUser_success() throws Exception {
        FetchUserResponse dto = FetchUserResponse.builder()
                .id(REQUESTER_ID)
                .email("carrot@carrot.com")
                .nickname(VALID_NICKNAME)
                .isMine(true)
                .build();
        when(userService.fetchUser(REQUESTER_ID, REQUESTER_ID)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/users/{userId}", REQUESTER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.data.email").value("carrot@carrot.com"))
                .andExpect(jsonPath("$.data.isMine").value(true));
    }

    @Test
    @DisplayName("올바른 값으로 수정 요청하면 200을 반환하고 서비스를 호출한다")
    void updateUser_success() throws Exception {
        mockMvc.perform(put("/api/v1/users/{userId}", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"%s","confirmPassword":"%s","nickname":"%s"}
                                """.formatted(VALID_PASSWORD, VALID_PASSWORD, VALID_NICKNAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"));

        verify(userService).updateUser(eq(REQUESTER_ID), eq(REQUESTER_ID), any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 조회하면 400과 함께 USER_NOT_EXIST 코드를 반환한다")
    void fetchUser_fail_whenUserNotExist() throws Exception {
        when(userService.fetchUser(REQUESTER_ID, REQUESTER_ID)).thenThrow(new CustomException(USER_NOT_EXIST));

        mockMvc.perform(get("/api/v1/users/{userId}", REQUESTER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(USER_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("존재하지 않는 사용자를 수정하면 400과 함께 USER_NOT_EXIST 코드를 반환한다")
    void updateUser_fail_whenUserNotExist() throws Exception {
        doThrow(new CustomException(USER_NOT_EXIST)).when(userService).updateUser(eq(REQUESTER_ID), eq(REQUESTER_ID), any());

        mockMvc.perform(put("/api/v1/users/{userId}", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"%s","confirmPassword":"%s","nickname":"%s"}
                                """.formatted(VALID_PASSWORD, VALID_PASSWORD, VALID_NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(USER_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("본인이 아닌 사용자를 수정하려 하면 401과 함께 UPDATE_USER_NOT_AUTHORIZE 코드를 반환한다")
    void updateUser_fail_whenRequesterIsNotOwner() throws Exception {
        doThrow(new CustomException(UPDATE_USER_NOT_AUTHORIZE)).when(userService).updateUser(eq(REQUESTER_ID), eq(REQUESTER_ID), any());

        mockMvc.perform(put("/api/v1/users/{userId}", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"%s","confirmPassword":"%s","nickname":"%s"}
                                """.formatted(VALID_PASSWORD, VALID_PASSWORD, VALID_NICKNAME)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(UPDATE_USER_NOT_AUTHORIZE.getCode()));
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 다르면 400과 함께 PASSWORD_CONFIRM_NOT_MATCH 코드를 반환한다")
    void updateUser_fail_whenPasswordConfirmMismatch() throws Exception {
        doThrow(new CustomException(PASSWORD_CONFIRM_NOT_MATCH)).when(userService).updateUser(eq(REQUESTER_ID), eq(REQUESTER_ID), any());

        mockMvc.perform(put("/api/v1/users/{userId}", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"%s","confirmPassword":"otherPassword1!","nickname":"%s"}
                                """.formatted(VALID_PASSWORD, VALID_NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(PASSWORD_CONFIRM_NOT_MATCH.getCode()));
    }

    @ParameterizedTest(name = "password=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("수정 요청의 비밀번호가 비어있거나 형식이 잘못되면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    @ValueSource(strings = {"short1!", "password", "12345678"})
    void updateUser_fail_whenPasswordInvalid(String invalidPassword) throws Exception {
        String passwordJson = invalidPassword == null ? "null" : "\"" + invalidPassword + "\"";

        mockMvc.perform(put("/api/v1/users/{userId}", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":%s,"confirmPassword":%s,"nickname":"%s"}
                                """.formatted(passwordJson, passwordJson, VALID_NICKNAME)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                // 빈 문자열은 @NotBlank/@Size/@Pattern이 동시에 걸려 같은 필드가 중복으로 나올 수 있어
                // 정확한 개수 대신 "password/confirmPassword 외 다른 필드는 없어야 한다"만 확인한다.
                .andExpect(jsonPath("$.data[*].field", everyItem(isIn(List.of("password", "confirmPassword")))))
                .andExpect(jsonPath("$.data[*].field", hasItem("password")))
                .andExpect(jsonPath("$.data[*].field", hasItem("confirmPassword")));
    }

    @ParameterizedTest(name = "nickname=\"{0}\" 이면 검증 실패(400, VF)를 반환한다")
    @DisplayName("수정 요청의 닉네임이 비어있으면 검증 실패(400, VF)를 반환한다")
    @NullAndEmptySource
    void updateUser_fail_whenNicknameBlank(String invalidNickname) throws Exception {
        String nicknameJson = invalidNickname == null ? "null" : "\"" + invalidNickname + "\"";

        mockMvc.perform(put("/api/v1/users/{userId}", REQUESTER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"%s","confirmPassword":"%s","nickname":%s}
                                """.formatted(VALID_PASSWORD, VALID_PASSWORD, nicknameJson)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("nickname")));
    }

    @Test
    @DisplayName("올바른 좌표로 주소 인증을 요청하면 200을 반환하고 서비스를 호출한다")
    void verifyAddress_success() throws Exception {
        mockMvc.perform(post("/api/v1/users/verify/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longitude":"127.0","latitude":"37.5"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SC"));

        verify(userService).verifyAddress(any(), eq(REQUESTER_ID));
    }

    @Test
    @DisplayName("존재하지 않는 사용자가 주소 인증을 요청하면 400과 함께 USER_NOT_EXIST 코드를 반환한다")
    void verifyAddress_fail_whenUserNotExist() throws Exception {
        doThrow(new CustomException(USER_NOT_EXIST)).when(userService).verifyAddress(any(), eq(REQUESTER_ID));

        mockMvc.perform(post("/api/v1/users/verify/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longitude":"127.0","latitude":"37.5"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(USER_NOT_EXIST.getCode()));
    }

    @Test
    @DisplayName("경도가 비어있으면 검증 실패(400, VF)를 반환한다 - VerifyAddressUserRequest에 @NotBlank 적용 확인")
    void verifyAddress_fail_whenLongitudeBlank() throws Exception {
        mockMvc.perform(post("/api/v1/users/verify/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longitude":"","latitude":"37.5"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("longitude")));
    }

    @Test
    @DisplayName("위도가 비어있으면 검증 실패(400, VF)를 반환한다 - VerifyAddressUserRequest에 @NotBlank 적용 확인")
    void verifyAddress_fail_whenLatitudeBlank() throws Exception {
        mockMvc.perform(post("/api/v1/users/verify/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"longitude":"127.0","latitude":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.data[*].field", hasItem("latitude")));
    }

    @TestConfiguration
    static class ArgumentResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
