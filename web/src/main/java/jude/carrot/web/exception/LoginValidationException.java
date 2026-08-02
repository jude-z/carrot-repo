package jude.carrot.web.exception;


import lombok.Getter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;

@Getter
public class LoginValidationException extends AuthenticationException {

    private final BindingResult bindingResult;

    public LoginValidationException(BindingResult bindingResult) {
        super("login request validation failed");
        this.bindingResult = bindingResult;
    }
}
