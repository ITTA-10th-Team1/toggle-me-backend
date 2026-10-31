package com.itta.toggleme.image.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ImageAnalysisBusyException extends BusinessException {

    public ImageAnalysisBusyException() {
        super(ErrorCode.IMAGE_ANALYSIS_BUSY);
    }
}
