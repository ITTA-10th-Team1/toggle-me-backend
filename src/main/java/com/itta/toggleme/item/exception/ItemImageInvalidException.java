package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemImageInvalidException extends BusinessException {

    public ItemImageInvalidException() {
        super(ErrorCode.ITEM_IMAGE_INVALID);
    }
}
