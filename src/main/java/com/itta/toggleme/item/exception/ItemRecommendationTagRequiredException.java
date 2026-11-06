package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ItemRecommendationTagRequiredException extends BusinessException {

    public ItemRecommendationTagRequiredException() {
        super(ErrorCode.ITEM_RECOMMENDATION_TAG_REQUIRED);
    }
}
