package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemRecommendationLimitExceededException extends BusinessException {

    public ItemRecommendationLimitExceededException() {
        super(ErrorCode.ITEM_RECOMMENDATION_LIMIT_EXCEEDED);
    }
}
