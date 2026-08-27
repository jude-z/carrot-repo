package jude.carrot.apiserver.domain.user.service;

import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import jude.carrot.infra.repository.image.ImageRepository;
import jude.carrot.infra.repository.user.UserRepository;
import jude.carrot.service.exception.CustomException;
import jude.carrot.web.client.KakaoClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static jude.carrot.apiserver.domain.user.fixture.request.RequestFactory.createSignUpRequest;
import static jude.carrot.apiserver.domain.user.fixture.request.RequestFactory.createUpdateUserRequest;
import static jude.carrot.apiserver.domain.user.fixture.request.RequestFactory.createVerifyAddressUserRequest;
import static jude.carrot.apiserver.domain.user.request.UserRequest.SignUpRequest;
import static jude.carrot.apiserver.domain.user.request.UserRequest.UpdateUserRequest;
import static jude.carrot.apiserver.domain.user.request.UserRequest.VerifyAddressUserRequest;
import static jude.carrot.apiserver.domain.user.response.UserResponse.FetchUserResponse;
import static jude.carrot.service.status.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String EMAIL = "carrot@carrot.com";
    private static final String RAW_PASSWORD = "password1!";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String NICKNAME = "carrot";
    private static final String PROFILE_IMAGE_URL = "http://image.com/profile.png";

    @InjectMocks
    UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private KakaoClient kakaoClient;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.from(EMAIL, ENCODED_PASSWORD, NICKNAME, null);
        existingUser.setId(1L);
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 회원가입은 CustomException(USER_EXIST)을 던진다")
    void signUp_fail_whenEmailAlreadyExists() {
        SignUpRequest request = createSignUpRequest(EMAIL, RAW_PASSWORD);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_EXIST.getHttpStatus());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 다르면 CustomException(PASSWORD_CONFIRM_NOT_MATCH)을 던진다")
    void signUp_fail_whenPasswordConfirmMismatch() {
        SignUpRequest request = createSignUpRequest(EMAIL, RAW_PASSWORD, "otherPassword1!");

        assertThatThrownBy(() -> userService.signUp(request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(PASSWORD_CONFIRM_NOT_MATCH.getHttpStatus());

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("가입되지 않은 이메일이면 비밀번호를 인코딩하여 회원을 저장한다")
    void signUp_success() {
        SignUpRequest request = SignUpRequest.builder()
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .confirmPassword(RAW_PASSWORD)
                .nickname(NICKNAME)
                .profileImageUrl(PROFILE_IMAGE_URL)
                .build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userService.signUp(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(savedUser.getNickname()).isEqualTo(NICKNAME);
        assertThat(savedUser.getImage().getUrl()).isEqualTo(PROFILE_IMAGE_URL);
    }

    @Test
    @DisplayName("존재하지 않는 회원을 조회하면 CustomException(USER_NOT_EXIST)을 던진다")
    void fetchUser_fail_whenUserNotExist() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.fetchUser(1L, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("본인이 조회하면 비밀번호와 인증여부를 포함한 응답을 반환한다")
    void fetchUser_success_whenRequesterIsOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        FetchUserResponse response = userService.fetchUser(1L, 1L);

        assertThat(response.isMine()).isTrue();
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.password()).isEqualTo(ENCODED_PASSWORD);
        assertThat(response.verified()).isFalse();
    }

    @Test
    @DisplayName("타인이 조회하면 비밀번호와 인증여부는 응답에 포함되지 않는다")
    void fetchUser_success_whenRequesterIsNotOwner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        FetchUserResponse response = userService.fetchUser(1L, 2L);

        assertThat(response.isMine()).isFalse();
        assertThat(response.password()).isNull();
        assertThat(response.verified()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 회원을 수정하면 CustomException(USER_NOT_EXIST)을 던진다")
    void updateUser_fail_whenUserNotExist() {
        UpdateUserRequest request = createUpdateUserRequest(RAW_PASSWORD, NICKNAME, PROFILE_IMAGE_URL);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUser(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("본인이 아닌 회원이 수정을 시도하면 CustomException(UPDATE_USER_NOT_AUTHORIZE)을 던진다")
    void updateUser_fail_whenRequesterIsNotOwner() {
        UpdateUserRequest request = createUpdateUserRequest(RAW_PASSWORD, NICKNAME, PROFILE_IMAGE_URL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.updateUser(1L, 2L, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(UPDATE_USER_NOT_AUTHORIZE.getHttpStatus());

        verify(imageRepository, never()).save(any(SingleImage.class));
    }

    @Test
    @DisplayName("수정 요청의 비밀번호와 비밀번호 확인이 다르면 CustomException(PASSWORD_CONFIRM_NOT_MATCH)을 던진다")
    void updateUser_fail_whenPasswordConfirmMismatch() {
        UpdateUserRequest request = createUpdateUserRequest(RAW_PASSWORD, "otherPassword1!", NICKNAME, PROFILE_IMAGE_URL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.updateUser(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(PASSWORD_CONFIRM_NOT_MATCH.getHttpStatus());

        verify(imageRepository, never()).save(any(SingleImage.class));
    }

    @Test
    @DisplayName("프로필 이미지 URL이 새로운 값이면 새 이미지를 저장하고 회원 정보를 갱신한다")
    void updateUser_success_whenProfileImageChanged() {
        String newPassword = "newPassword1!";
        String newEncodedPassword = "new-encoded-password";
        UpdateUserRequest request = createUpdateUserRequest(newPassword, "newNickname", PROFILE_IMAGE_URL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);

        userService.updateUser(1L, 1L, request);

        ArgumentCaptor<SingleImage> imageCaptor = ArgumentCaptor.forClass(SingleImage.class);
        verify(imageRepository).save(imageCaptor.capture());
        assertThat(imageCaptor.getValue().getUrl()).isEqualTo(PROFILE_IMAGE_URL);
        assertThat(existingUser.getImage().getUrl()).isEqualTo(PROFILE_IMAGE_URL);
        assertThat(existingUser.getPassword()).isEqualTo(newEncodedPassword);
        assertThat(existingUser.getNickname()).isEqualTo("newNickname");
    }

    @Test
    @DisplayName("프로필 이미지 URL이 기존과 동일하면 이미지를 다시 저장하지 않는다")
    void updateUser_success_whenProfileImageUnchanged() {
        existingUser.setImage(SingleImage.from(PROFILE_IMAGE_URL));
        UpdateUserRequest request = createUpdateUserRequest(RAW_PASSWORD, NICKNAME, PROFILE_IMAGE_URL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userService.updateUser(1L, 1L, request);

        verify(imageRepository, never()).save(any(SingleImage.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원의 주소를 인증하면 CustomException(USER_NOT_EXIST)을 던진다")
    void verifyAddress_fail_whenUserNotExist() {
        VerifyAddressUserRequest request = createVerifyAddressUserRequest("127.0", "37.5");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyAddress(request, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(USER_NOT_EXIST.getHttpStatus());
    }

    @Test
    @DisplayName("경도 또는 위도가 비어있으면 CustomException(NOT_VALID_ADDRESS)을 던진다")
    void verifyAddress_fail_whenAddressNotValid() {
        VerifyAddressUserRequest request = createVerifyAddressUserRequest("", "37.5");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> userService.verifyAddress(request, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("httpStatus")
                .isEqualTo(NOT_VALID_ADDRESS.getHttpStatus());

        verify(kakaoClient, never()).fetchAddress(anyString(), anyString());
    }

    @Test
    @DisplayName("유효한 좌표이면 카카오 API로 주소를 조회하여 회원 주소를 인증 처리한다")
    void verifyAddress_success() {
        String longitude = "127.0";
        String latitude = "37.5";
        Address address = Address.from("서울시", "강남구", "역삼동");
        VerifyAddressUserRequest request = createVerifyAddressUserRequest(longitude, latitude);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(kakaoClient.fetchAddress(longitude, latitude)).thenReturn(address);

        userService.verifyAddress(request, 1L);

        assertThat(existingUser.getAddress()).isEqualTo(address);
        assertThat(existingUser.isVerified()).isTrue();
    }

}
