package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class TasteProfileNotEnoughException extends BusinessException {

    public TasteProfileNotEnoughException() {
        super(ErrorCode.TASTE_PROFILE_NOT_ENOUGH);
    }
}
