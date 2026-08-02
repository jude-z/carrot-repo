package jude.carrot.infra.fixture.user;


import jude.carrot.infra.entity.user.User;

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
