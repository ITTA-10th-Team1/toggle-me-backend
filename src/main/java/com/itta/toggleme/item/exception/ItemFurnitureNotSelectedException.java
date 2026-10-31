package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemFurnitureNotSelectedException extends BusinessException {

    public ItemFurnitureNotSelectedException() {
        super(ErrorCode.ITEM_FURNITURE_NOT_SELECTED);
    }
}
