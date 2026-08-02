package jude.carrot.web.filter;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jude.carrot.web.exception.LoginValidationException;
import jude.carrot.web.request.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Validator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;


@Slf4j
@Component
public class JsonAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final ObjectMapper mapper;
    private final Validator validator;
    private final String loginUrl;

    public JsonAuthenticationFilter(ObjectMapper mapper,
                                    Validator validator,
                                    AuthenticationManager authenticationManager,
                                    @Value("${jude.auth.default-url}") String defaultUrl,
                                    @Value("${jude.auth.login-url}") String loginUrl
    ){
        super();
        this.mapper = mapper;
        this.validator = validator;
        this.loginUrl = defaultUrl + loginUrl;
        setFilterProcessesUrl(this.loginUrl);
        setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        if (!request.getMethod().equals("POST")) {
            throw new AuthenticationServiceException("Authentication method not supported: " + request.getMethod());
        }
        LoginRequest loginRequest = parse(request, LoginRequest.class);
        if (loginRequest == null) {
            loginRequest = new LoginRequest();
        }
        validate(loginRequest);
        String username = fetchUsername(loginRequest);
        String password = fetchPassword(loginRequest);
        UsernamePasswordAuthenticationToken authRequest = UsernamePasswordAuthenticationToken.unauthenticated(username,
                password);
        setDetails(request, authRequest);
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    private void validate(LoginRequest loginRequest) {
        BindException bindingResult = new BindException(loginRequest, "loginRequest");
        validator.validate(loginRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new LoginValidationException(bindingResult);
        }
    }

    private <T> T parse(HttpServletRequest request, Class<T> clazz){
        try (InputStream inputStream = request.getInputStream()){
            return mapper.readValue(inputStream,clazz);
        }catch (IOException | JacksonException e){
            log.error("exception", e);
            return null;
        }
    }
    private String fetchUsername(LoginRequest loginRequest){

        String email = loginRequest.getEmail();
        return StringUtils.hasText(email) ? email : "";
    }

    private String fetchPassword(LoginRequest loginRequest){
        String password = loginRequest.getPassword();
        return StringUtils.hasText(password) ? password : "";
    }
}
