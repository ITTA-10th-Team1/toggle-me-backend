package com.itta.toggleme.member.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class HandleReservedException extends BusinessException {
    public HandleReservedException() { super(ErrorCode.HANDLE_RESERVED); }
}
