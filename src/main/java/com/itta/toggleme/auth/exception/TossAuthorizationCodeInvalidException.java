package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class TossAuthorizationCodeInvalidException extends BusinessException {

    public TossAuthorizationCodeInvalidException() {
        super(ErrorCode.TOSS_AUTHORIZATION_CODE_INVALID);
    }
}
