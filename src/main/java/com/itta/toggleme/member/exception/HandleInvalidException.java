package com.itta.toggleme.member.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class HandleInvalidException extends BusinessException {
    public HandleInvalidException() { super(ErrorCode.HANDLE_INVALID); }
}
