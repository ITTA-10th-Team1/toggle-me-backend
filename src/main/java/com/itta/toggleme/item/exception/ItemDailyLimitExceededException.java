package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemDailyLimitExceededException extends BusinessException {

    public ItemDailyLimitExceededException() {
        super(ErrorCode.ITEM_DAILY_LIMIT_EXCEEDED);
    }
}
