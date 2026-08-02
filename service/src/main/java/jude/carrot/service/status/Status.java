package jude.carrot.service.status;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum Status {
    SUCCESS("SC", "success", HttpStatus.OK),
    USER_EXIST("UE", "user already exists", HttpStatus.CONFLICT),
    USER_NOT_EXIST("UE", "user already exists", HttpStatus.BAD_REQUEST),
    VALID_FAIL("VF","valid fail",HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_CORRECT("PNC", "password not correct", HttpStatus.BAD_REQUEST),
    AUTH_FAIL("AF","authentication fail",HttpStatus.UNAUTHORIZED),
    SECURITY_ERROR("SE","security error",HttpStatus.INTERNAL_SERVER_ERROR),
    UPDATE_USER_NOT_AUTHORIZE("NF", "cannot update other user", HttpStatus.UNAUTHORIZED),
    EXTERNAL_SERVER_ERROR("ESE", "server error", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_VALID_ADDRESS("NVA","not valid address" , HttpStatus.BAD_REQUEST),
    CHAT_ROOM_NOT_EXIST("CRE", "chat room does not exist", HttpStatus.BAD_REQUEST),
    CHAT_PARTICIPANT_NOT_EXIST("CPE", "chat participant does not exist", HttpStatus.BAD_REQUEST),
    CHAT_ROOM_NOT_AUTHORIZE("CRA", "not a participant of this chat room", HttpStatus.UNAUTHORIZED),
    POST_NOT_EXIST("PNE", "post not exist", HttpStatus.BAD_REQUEST),
    ADDRESS_NOT_ENROLLED("ANE", "address not enrolled", HttpStatus.BAD_REQUEST),
    UPDATE_POST_NOT_AUTHORIZE("UPN", "this user is not creater", HttpStatus.BAD_REQUEST),
    CHAT_MESSAGE_NOT_EXIST("CNE", "chat message not exit",HttpStatus.BAD_REQUEST);

    private final String code;
    private final String detailMessage;
    private final HttpStatus httpStatus;
}
