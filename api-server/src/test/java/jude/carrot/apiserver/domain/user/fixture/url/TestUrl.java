package jude.carrot.apiserver.domain.user.fixture.url;

public enum TestUrl {

    SIGN_UP("/api/v1/auth/signup"),
    LOGIN("/api/v1/auth/login"),
    USERS("/api/v1/users"),
    VERIFY_ADDRESS("/api/v1/users/verify/address");

    private final String url;

    TestUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
