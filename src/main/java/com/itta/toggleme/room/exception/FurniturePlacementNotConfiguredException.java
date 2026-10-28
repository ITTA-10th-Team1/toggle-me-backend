package com.itta.toggleme.room.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class FurniturePlacementNotConfiguredException extends BusinessException {

    public FurniturePlacementNotConfiguredException() {
        super(ErrorCode.FURNITURE_PLACEMENT_NOT_CONFIGURED);
    }
}
