package jude.carrot.web.auth;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jude.carrot.service.status.Status;
import jude.carrot.web.dto.ValidFailDto;
import jude.carrot.web.exception.LoginValidationException;
import jude.carrot.web.response.ApiResponse;
import jude.carrot.web.mapper.ValidFailDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static jude.carrot.service.status.Status.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final ObjectMapper mapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (exception instanceof LoginValidationException validationException) {
            List<ValidFailDto> data = ValidFailDtoMapper.from(validationException.getBindingResult());
            writeResponse(response, VALID_FAIL.getHttpStatus().value(),
                    ApiResponse.failFrom(data, VALID_FAIL.getCode(), VALID_FAIL.getDetailMessage()));
            return;
        }
        Status status = exception instanceof BadCredentialsException ? AUTH_FAIL : SECURITY_ERROR;
        if (status == SECURITY_ERROR) {
            log.error("unexpected authentication exception", exception);
        }
        writeResponse(response, status.getHttpStatus().value(),
                ApiResponse.failFrom(status.getCode(), status.getDetailMessage()));
    }

    private void writeResponse(HttpServletResponse response, int httpStatus, ApiResponse<?> apiResponse) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(mapper.writeValueAsString(apiResponse));
    }
}
