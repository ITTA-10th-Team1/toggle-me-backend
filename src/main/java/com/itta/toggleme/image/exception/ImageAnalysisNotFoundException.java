package com.itta.toggleme.image.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ImageAnalysisNotFoundException extends BusinessException {

    public ImageAnalysisNotFoundException() {
        super(ErrorCode.IMAGE_ANALYSIS_NOT_FOUND);
    }
}
