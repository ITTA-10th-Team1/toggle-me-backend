package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class EmailVerificationTooManyAttemptsException extends BusinessException {

    public EmailVerificationTooManyAttemptsException() {
        super(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_ATTEMPTS);
    }
}
