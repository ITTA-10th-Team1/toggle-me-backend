package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemRecommendationUnsupportedException extends BusinessException {

    public ItemRecommendationUnsupportedException() {
        super(ErrorCode.ITEM_RECOMMENDATION_UNSUPPORTED);
    }
}
