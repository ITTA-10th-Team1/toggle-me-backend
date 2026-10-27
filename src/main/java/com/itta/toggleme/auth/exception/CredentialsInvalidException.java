package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class CredentialsInvalidException extends BusinessException {

    public CredentialsInvalidException() {
        super(ErrorCode.CREDENTIALS_INVALID);
    }
}
