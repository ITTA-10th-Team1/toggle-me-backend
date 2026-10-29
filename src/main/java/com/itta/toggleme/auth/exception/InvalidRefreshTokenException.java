package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class InvalidRefreshTokenException extends BusinessException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
