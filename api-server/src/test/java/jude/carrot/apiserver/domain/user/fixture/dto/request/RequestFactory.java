package jude.carrot.apiserver.domain.user.fixture.dto.request;


import jude.carrot.apiserver.domain.user.dto.request.UserRequest;

import static jude.carrot.apiserver.domain.user.dto.request.UserRequest.*;

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
}
