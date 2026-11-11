package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class TossCallbackUnauthorizedException extends BusinessException {

    public TossCallbackUnauthorizedException() {
        super(ErrorCode.TOSS_CALLBACK_UNAUTHORIZED);
    }
}
