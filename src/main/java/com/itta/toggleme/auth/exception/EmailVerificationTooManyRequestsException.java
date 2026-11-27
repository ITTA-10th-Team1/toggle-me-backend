package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class EmailVerificationTooManyRequestsException extends BusinessException {

    public EmailVerificationTooManyRequestsException() {
        super(ErrorCode.EMAIL_VERIFICATION_TOO_MANY_REQUESTS);
    }
}
