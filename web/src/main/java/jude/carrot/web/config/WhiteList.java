package jude.carrot.web.config;

import lombok.Getter;

@Getter
public enum WhiteList {
    HEALTH("/health","GET"),
    CHECK("/check","GET"),
    LOGIN("/api/v1/auth/login","POST"),
    SIGNUP("/api/v1/auth/signup","POST"),
    ACTUATOR("/actuator/**","GET");
    private final String url;
    private final String method;

    WhiteList(String url,String method) {
        this.url = url;
        this.method = method;
    }
}
