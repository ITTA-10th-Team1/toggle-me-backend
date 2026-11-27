package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class EmailVerificationCodeExpiredException extends BusinessException {

    public EmailVerificationCodeExpiredException() {
        super(ErrorCode.EMAIL_VERIFICATION_CODE_EXPIRED);
    }
}
