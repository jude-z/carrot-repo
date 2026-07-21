package jude.carrot.apiserver.domain.user.fixture.urls;

public enum TestUrl {

    SIGN_UP("/signup");

    private final String url;

    TestUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
