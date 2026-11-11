package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class TossApiException extends BusinessException {

    public TossApiException() {
        super(ErrorCode.TOSS_API_ERROR);
    }
}
