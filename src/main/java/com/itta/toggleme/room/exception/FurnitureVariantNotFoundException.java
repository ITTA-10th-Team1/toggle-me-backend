package com.itta.toggleme.room.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class FurnitureVariantNotFoundException extends BusinessException {

    public FurnitureVariantNotFoundException() {
        super(ErrorCode.FURNITURE_VARIANT_NOT_FOUND);
    }
}
