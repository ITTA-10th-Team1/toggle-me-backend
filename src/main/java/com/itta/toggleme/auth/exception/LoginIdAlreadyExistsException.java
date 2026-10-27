package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class LoginIdAlreadyExistsException extends BusinessException {

    public LoginIdAlreadyExistsException() {
        super(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
    }
}
