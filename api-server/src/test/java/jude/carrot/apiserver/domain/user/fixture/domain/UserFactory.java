package jude.carrot.apiserver.domain.user.fixture.domain;

import jude.carrot.apiserver.domain.user.User;

public class UserFactory {

    private UserFactory() {
    }

    public static User create(String email){
        return User.builder()
                .email(email)
                .password("password")
                .build();
    }
}
