package com.itta.toggleme.member.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class HandleGenerationFailedException extends BusinessException {

    public HandleGenerationFailedException() {
        super(ErrorCode.HANDLE_GENERATION_FAILED);
    }
}
