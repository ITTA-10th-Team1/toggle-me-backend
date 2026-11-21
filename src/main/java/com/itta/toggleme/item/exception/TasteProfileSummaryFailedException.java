package com.itta.toggleme.item.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class TasteProfileSummaryFailedException extends BusinessException {

    public TasteProfileSummaryFailedException() {
        super(ErrorCode.TASTE_PROFILE_SUMMARY_FAILED);
    }
}
