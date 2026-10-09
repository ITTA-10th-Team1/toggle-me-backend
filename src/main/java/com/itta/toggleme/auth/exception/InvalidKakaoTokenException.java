package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class InvalidKakaoTokenException extends BusinessException {

    public InvalidKakaoTokenException() {
        super(ErrorCode.INVALID_KAKAO_TOKEN);
    }
}
