package com.itta.toggleme.member.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class HandleAlreadyExistsException extends BusinessException {
    public HandleAlreadyExistsException() { super(ErrorCode.HANDLE_ALREADY_EXISTS); }
}
