package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemTagInvalidException extends BusinessException {

    public ItemTagInvalidException() {
        super(ErrorCode.ITEM_TAG_INVALID);
    }
}
