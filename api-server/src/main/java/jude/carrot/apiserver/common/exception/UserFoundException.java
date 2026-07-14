package jude.carrot.apiserver.common.exception;

import jude.carrot.apiserver.common.status.Status;
import lombok.Getter;

@Getter
public class UserFoundException extends CustomException {
    public UserFoundException(Status status){
        super(status);
    }
}
