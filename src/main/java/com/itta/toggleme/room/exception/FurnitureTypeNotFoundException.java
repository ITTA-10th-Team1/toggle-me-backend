package com.itta.toggleme.room.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class FurnitureTypeNotFoundException extends BusinessException {

    public FurnitureTypeNotFoundException() {
        super(ErrorCode.FURNITURE_TYPE_NOT_FOUND);
    }
}
