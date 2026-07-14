package jude.carrot.apiserver.user.service;

import jude.carrot.apiserver.common.exception.UserFoundException;
import jude.carrot.apiserver.user.domain.User;
import jude.carrot.apiserver.user.dto.request.UserRequest;
import jude.carrot.apiserver.user.dto.request.UserRequest.SignUpRequest;
import jude.carrot.apiserver.user.fixture.domain.UserFactory;
import jude.carrot.apiserver.user.fixture.dto.request.RequestFactory;
import jude.carrot.apiserver.user.repository.UserRepository;
import jude.carrot.apiserver.user.repository.UserRepositoryImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    UserService userService;
    @Mock
    UserRepository userRepository;
    @Mock
    BCryptPasswordEncoder passwordEncoder;

    static String EXIST_EMAIL = "true@gmail.com";
    static String NON_EXIST_EMAIL = "fake@gmail.com";
    static String RAW_PASSWORD = "password";
    static String ENCODED_PASSWORD = "encodedPassword";
    @Test
    void signUpThrowException() {
        SignUpRequest signUpRequest = RequestFactory.createSignUpRequest(EXIST_EMAIL, RAW_PASSWORD);
        User user = UserFactory.create(EXIST_EMAIL);
        when(userRepository.findUser(EXIST_EMAIL)).thenReturn(Optional.of(user));

        Assertions.assertThatThrownBy(() -> userService.signUp(signUpRequest))
                .isInstanceOf(UserFoundException.class);
    }

    @Test
    void signUp(){
        SignUpRequest signUpRequest = RequestFactory.createSignUpRequest(NON_EXIST_EMAIL, RAW_PASSWORD);
        when(userRepository.findUser(NON_EXIST_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn(ENCODED_PASSWORD);
        userService.signUp(signUpRequest);
        String email = signUpRequest.getEmail();
        String password = signUpRequest.getPassword();
        verify(passwordEncoder,times(1)).encode(password);
        verify(userRepository,times(1)).save(email,ENCODED_PASSWORD);
    }
}