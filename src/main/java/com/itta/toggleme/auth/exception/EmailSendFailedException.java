package com.itta.toggleme.auth.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class EmailSendFailedException extends BusinessException {

    public EmailSendFailedException() {
        super(ErrorCode.EMAIL_SEND_FAILED);
    }
}
