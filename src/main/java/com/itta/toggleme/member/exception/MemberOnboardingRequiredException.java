package com.itta.toggleme.member.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class MemberOnboardingRequiredException extends BusinessException {
    public MemberOnboardingRequiredException() { super(ErrorCode.MEMBER_ONBOARDING_REQUIRED); }
}
