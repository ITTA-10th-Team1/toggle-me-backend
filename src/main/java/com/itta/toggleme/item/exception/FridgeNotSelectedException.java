package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class FridgeNotSelectedException extends BusinessException {

    public FridgeNotSelectedException() {
        super(ErrorCode.FRIDGE_NOT_SELECTED);
    }
}
