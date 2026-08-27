package jude.carrot.apiserver.domain.user.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import jude.carrot.infra.entity.image.SingleImage;
import jude.carrot.infra.entity.user.Address;
import jude.carrot.infra.entity.user.User;
import lombok.Builder;
import org.springframework.util.StringUtils;

public class UserResponse {
    private UserResponse(){}

    @Builder
    public record FetchUserResponse(
            Long id,
            String email,
            @JsonInclude(value = JsonInclude.Include.NON_NULL) String password,
            String nickname,
            @JsonInclude(value = JsonInclude.Include.NON_NULL) String profileImageUrl,
            Address address,
            Boolean verified,
            Boolean isMine
    ) {

        public static FetchUserResponse from(User user, boolean isMine){
            SingleImage image = user.getImage();
            return FetchUserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .password(isMine ? user.getPassword() : null)
                    .nickname(user.getNickname())
                    .profileImageUrl(image == null || !StringUtils.hasText(image.getUrl()) ? null : image.getUrl())
                    .address(user.getAddress())
                    .verified(isMine ? user.isVerified() : null)
                    .isMine(isMine)
                    .build();
        }
    }
}
