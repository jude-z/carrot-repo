package jude.carrot.apiserver.common.exception;

import jude.carrot.apiserver.common.status.Status;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public class CustomException extends RuntimeException {

    private final String code;
    private final String detailMessage;
    private final HttpStatus httpStatus;

    public CustomException(Status status){
        this.code = status.getCode();
        this.detailMessage = status.getDetailMessage();
        this.httpStatus = status.getHttpStatus();
    }
}
