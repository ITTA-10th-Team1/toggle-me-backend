package com.itta.toggleme.room.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class InvalidFurnitureSelectionException extends BusinessException {

    public InvalidFurnitureSelectionException() {
        super(ErrorCode.INVALID_FURNITURE_SELECTION);
    }
}
