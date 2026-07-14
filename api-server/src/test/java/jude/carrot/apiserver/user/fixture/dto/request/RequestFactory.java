package jude.carrot.apiserver.user.fixture.dto.request;


import static jude.carrot.apiserver.user.dto.request.UserRequest.*;

public class RequestFactory {
    public static SignUpRequest createSignUpRequest(String email, String password){
        return SignUpRequest.builder()
                .email(email)
                .password(password)
                .confirmPassword(password)
                .build();
    }
}
