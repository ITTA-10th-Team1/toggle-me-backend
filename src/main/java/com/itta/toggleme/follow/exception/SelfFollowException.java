package com.itta.toggleme.follow.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class SelfFollowException extends BusinessException {

    public SelfFollowException() {
        super(ErrorCode.FOLLOW_SELF);
    }
}
