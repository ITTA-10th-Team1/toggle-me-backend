package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class TasteProfileLimitExceededException extends BusinessException {

    public TasteProfileLimitExceededException() {
        super(ErrorCode.TASTE_PROFILE_LIMIT_EXCEEDED);
    }
}
