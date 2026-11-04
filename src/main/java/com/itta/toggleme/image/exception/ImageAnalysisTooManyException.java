package com.itta.toggleme.image.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ImageAnalysisTooManyException extends BusinessException {

    public ImageAnalysisTooManyException() {
        super(ErrorCode.IMAGE_ANALYSIS_TOO_MANY);
    }
}
