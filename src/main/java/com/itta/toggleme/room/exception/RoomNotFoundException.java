package com.itta.toggleme.room.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class RoomNotFoundException extends BusinessException {

    public RoomNotFoundException() {
        super(ErrorCode.ROOM_NOT_FOUND);
    }
}
