package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class EmailVerificationRequiredException extends BusinessException {

    public EmailVerificationRequiredException() {
        super(ErrorCode.EMAIL_VERIFICATION_REQUIRED);
    }
}
