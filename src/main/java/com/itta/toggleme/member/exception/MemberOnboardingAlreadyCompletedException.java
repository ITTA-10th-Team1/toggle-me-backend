package com.itta.toggleme.member.exception;

import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.global.exception.ErrorCode;

public class MemberOnboardingAlreadyCompletedException extends BusinessException {
    public MemberOnboardingAlreadyCompletedException() {
        super(ErrorCode.MEMBER_ONBOARDING_ALREADY_COMPLETED);
    }
}
