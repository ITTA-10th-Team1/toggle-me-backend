package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class KakaoApiException extends BusinessException {

    public KakaoApiException() {
        super(ErrorCode.KAKAO_API_ERROR);
    }
}
