package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class EmailVerificationCodeInvalidException extends BusinessException {

    public EmailVerificationCodeInvalidException() {
        super(ErrorCode.EMAIL_VERIFICATION_CODE_INVALID);
    }
}
