package jude.carrot.apiserver.domain.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jude.carrot.infra.entity.user.User;
import lombok.Builder;
import org.springframework.util.StringUtils;

import static jude.carrot.infra.entity.image.SingleImage.*;


public class UserRequest {

    private UserRequest() {
    }

    @Builder
    public record SignUpRequest(
            @NotBlank(message = "이메일을 입력해주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
                    message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
            )
            String password,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
                    message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
            )
            String confirmPassword,

            @NotBlank(message = "닉네임을 입력해주세요.")
            String nickname,

            String profileImageUrl
    ) {
    }


    @Builder
    public record UpdateUserRequest(
            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
                    message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
            )
            String password,

            @NotBlank(message = "비밀번호를 입력해주세요.")
            @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
            @Pattern(
                    regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
                    message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다."
            )
            String confirmPassword,

            @NotBlank(message = "닉네임을 입력해주세요.")
            String nickname,

            String profileImageUrl
    ) {

        public void updateUser(User user, String encodedPassword){
            user.setPassword(encodedPassword);
            user.setNickname(nickname);
            if(StringUtils.hasText(profileImageUrl)) user.setImage(from(profileImageUrl));
        }

    }


    @Builder
    public record VerifyAddressUserRequest(
            @NotBlank(message = "경도를 입력해주세요.")
            String longitude,

            @NotBlank(message = "위도를 입력해주세요.")
            String latitude
    ) {
    }

}
