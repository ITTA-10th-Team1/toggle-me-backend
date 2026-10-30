package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemRecordedDateInvalidException extends BusinessException {

    public ItemRecordedDateInvalidException() {
        super(ErrorCode.ITEM_RECORDED_DATE_INVALID);
    }
}
