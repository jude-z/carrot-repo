package jude.carrot.apiserver.domain.user.exception;

import jude.carrot.apiserver.common.exception.CustomException;
import jude.carrot.apiserver.common.status.Status;
import lombok.Getter;

@Getter
public class UserFoundException extends CustomException {
    public UserFoundException(Status status){
        super(status);
    }
}
