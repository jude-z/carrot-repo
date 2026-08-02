package jude.carrot.web.integration;

import com.redis.testcontainers.RedisContainer;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.user.UserRepository;
import jude.carrot.web.config.RedisSessionConfig;
import jude.carrot.web.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        SecurityConfig.class,
        RedisSessionConfig.class,
        DataRedisAutoConfiguration.class,
        SecurityWebMvcTest.TestConfig.class
})
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityWebMvcTest {

    @Container
    @ServiceConnection
    static final RedisContainer REDIS = new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PROTECTED_URL = "/api/v1/random-resource";
    private static final String EMAIL = "carrot@carrot.com";
    private static final String RAW_PASSWORD = "password1!";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final Long USER_ID = 1L;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private RedisSessionRepository sessionRepository;

    @Autowired
    private SessionRepositoryFilter<?> sessionRepositoryFilter;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private Validator validator;

    @MockitoBean
    private BCryptPasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(sessionRepositoryFilter)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("올바른 이메일/비밀번호로 로그인하면 세션 쿠키와 함께 성공 응답을 반환하고, Redis에 인증 세션이 실제로 저장된다")
    void login_success() throws Exception {
        stubUserFound();
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("SESSION"))
                .andExpect(jsonPath("$.code").value("SC"))
                .andExpect(jsonPath("$.detailMessage").value("success"))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        Cookie sessionCookie = result.getResponse().getCookie("SESSION");
        Session redisSession = findRedisSession(sessionCookie);

        assertThat(redisSession).as("생성된 세션이 Redis(sessionRepository)에 실제로 저장되어 있어야 한다").isNotNull();
        SecurityContext securityContext = redisSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(securityContext).isNotNull();
        assertThat(securityContext.getAuthentication().getName()).isEqualTo(String.valueOf(USER_ID));
        assertThat(securityContext.getAuthentication().isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 인증 실패(401, AF) 응답을 반환한다")
    void login_fail_whenUserNotFound() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, RAW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AF"))
                .andExpect(jsonPath("$.detailMessage").value("authentication fail"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 인증 실패(401, AF) 응답을 반환한다")
    void login_fail_whenPasswordMismatch() throws Exception {
        stubUserFound();
        // matches()는 스텁하지 않아 기본값(false)을 반환 -> 비밀번호 불일치 상황을 그대로 재현

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"wrongPassword1!"}
                                """.formatted(EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AF"))
                .andExpect(jsonPath("$.detailMessage").value("authentication fail"));
    }

    @Test
    @DisplayName("검증에 실패하면 필터가 예외를 던지고 잡아서 검증 실패(400, VF) 응답과 실패 필드 목록을 반환한다")
    void login_fail_whenRequestNotValid() throws Exception {
        doAnswer(invocation -> {
            Errors errors = invocation.getArgument(1);
            errors.rejectValue("email", "invalid", "이메일 형식이 올바르지 않습니다.");
            errors.rejectValue("password", "invalid", "비밀번호 형식이 올바르지 않습니다.");
            return null;
        }).when(validator).validate(any(), any(Errors.class));

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invalid-email","password":"12345678"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VF"))
                .andExpect(jsonPath("$.detailMessage").value("valid fail"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].field", containsInAnyOrder("email", "password")));
    }

    @Test
    @DisplayName("세션 없이 화이트리스트 밖의 리소스에 접근하면 차단된다")
    void anonymous_isBlocked_fromProtectedResource() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인 성공 후 발급된 세션으로 요청하면 인증된 사용자로 필터 체인을 통과한다")
    void authenticatedSession_passesThroughSecurityFilterChain() throws Exception {
        stubUserFound();
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        MvcResult loginResult = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(EMAIL, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie sessionCookie = loginResult.getResponse().getCookie("SESSION");

        // 인증된 세션은 인가 필터를 통과하고, 매핑된 컨트롤러가 없어 404로 끝난다 (403이 아님을 확인)
        mockMvc.perform(get(PROTECTED_URL).cookie(sessionCookie))
                .andExpect(status().isNotFound());

        // 같은 리소스라도 세션 쿠키가 없으면 인가 단계에서 차단된다
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isForbidden());
    }

    private void stubUserFound() {
        User user = User.from(EMAIL, ENCODED_PASSWORD, "carrot", null);
        user.setId(USER_ID);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
    }

    private Session findRedisSession(Cookie sessionCookie) {
        String sessionId = new String(Base64.getDecoder().decode(sessionCookie.getValue()));
        return sessionRepository.findById(sessionId);
    }

    @TestConfiguration
    @EnableWebSecurity
    @ComponentScan(basePackages = {"jude.carrot.web.auth", "jude.carrot.web.filter"})
    static class TestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
