package jude.carrot.apiserver.user.fixture.domain;

import jude.carrot.apiserver.user.domain.User;

public class UserFactory {
    public static User create(String email){
        return User.builder()
                .email(email)
                .password("password")
                .build();
    }
}
