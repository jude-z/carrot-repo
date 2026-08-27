package jude.carrot.apiserver.domain.user.fixture.request;


import jude.carrot.web.request.LoginRequest;

import static jude.carrot.apiserver.domain.user.request.UserRequest.*;

public class RequestFactory {

    private RequestFactory() {
    }

    public static SignUpRequest createSignUpRequest(String email, String password){
        return createSignUpRequest(email, password, password);
    }

    public static SignUpRequest createSignUpRequest(String email, String password, String confirmPassword){
        return SignUpRequest.builder()
                .email(email)
                .password(password)
                .confirmPassword(confirmPassword)
                .build();
    }

    public static LoginRequest createLoginRequest(String email, String password){
        return LoginRequest.builder()
                .email(email)
                .password(password)
                .build();
    }

    public static UpdateUserRequest createUpdateUserRequest(String password, String nickname, String profileImageUrl){
        return createUpdateUserRequest(password, password, nickname, profileImageUrl);
    }

    public static UpdateUserRequest createUpdateUserRequest(String password, String confirmPassword, String nickname, String profileImageUrl){
        return UpdateUserRequest.builder()
                .password(password)
                .confirmPassword(confirmPassword)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public static VerifyAddressUserRequest createVerifyAddressUserRequest(String longitude, String latitude){
        return VerifyAddressUserRequest.builder()
                .longitude(longitude)
                .latitude(latitude)
                .build();
    }
}
