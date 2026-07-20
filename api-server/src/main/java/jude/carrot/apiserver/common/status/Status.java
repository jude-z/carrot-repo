package jude.carrot.apiserver.common.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum Status {

    SUCCESS("SC", "success", HttpStatus.OK),
    USER_EXIST("UE", "user already exists", HttpStatus.CONFLICT),
    VALID_FAIL("VF","valid fail",HttpStatus.BAD_REQUEST);
    private final String code;
    private final String detailMessage;
    private final HttpStatus httpStatus;
}
