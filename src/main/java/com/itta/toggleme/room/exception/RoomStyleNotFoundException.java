package com.itta.toggleme.room.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class RoomStyleNotFoundException extends BusinessException {

    public RoomStyleNotFoundException() {
        super(ErrorCode.ROOM_STYLE_NOT_FOUND);
    }
}
