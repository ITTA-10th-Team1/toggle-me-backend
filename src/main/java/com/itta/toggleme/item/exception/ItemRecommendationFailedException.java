package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemRecommendationFailedException extends BusinessException {

    public ItemRecommendationFailedException() {
        super(ErrorCode.ITEM_RECOMMENDATION_FAILED);
    }
}
