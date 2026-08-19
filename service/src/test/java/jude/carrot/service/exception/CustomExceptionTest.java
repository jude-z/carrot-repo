package jude.carrot.service.exception;

import jude.carrot.service.status.Status;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {
    Status status;
    CustomException ex;
    @BeforeEach
    void setUp(){
        status = Status.CHAT_MESSAGE_NOT_EXIST;
        ex = new CustomException(status);
    }

    @Test
    void getCode() {
        String exceptionCode = ex.getCode();
        String statusCode = status.getCode();
        assertThat(exceptionCode).isEqualTo(statusCode);
    }

    @Test
    void getDetailMessage() {
        String exceptionMessage = ex.getDetailMessage();
        String statusMessage = status.getDetailMessage();
        assertThat(exceptionMessage).isEqualTo(statusMessage);
    }

    @Test
    void getHttpStatus() {
        HttpStatus exceptionHttpStatus = ex.getHttpStatus();
        HttpStatus statusHttpStatus = status.getHttpStatus();
        assertThat(exceptionHttpStatus).isEqualTo(statusHttpStatus);
    }
}