package com.itta.toggleme.member.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class MemberProfileRequiredException extends BusinessException {
    public MemberProfileRequiredException() { super(ErrorCode.MEMBER_PROFILE_REQUIRED); }
}
