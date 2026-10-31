package com.itta.toggleme.image.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class ImageInvalidException extends BusinessException {

    public ImageInvalidException() {
        super(ErrorCode.IMAGE_INVALID);
    }
}
