package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.client.TossClient;
import com.itta.toggleme.auth.client.TossUserInfo;
import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.member.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 토스 API 호출은 트랜잭션 밖에서 하고, 가입·로그인은 {@link SocialLoginService}에 맡긴다. */
@Service
@RequiredArgsConstructor
public class TossAuthService {

    private final TossClient tossClient;
    private final SocialLoginService socialLoginService;

    public LoginResponse loginWithToss(String authorizationCode, String referrer) {
        TossUserInfo tossUser = tossClient.getUserInfo(authorizationCode, referrer);
        // userKey 범위만 받아서 닉네임이 없다. 기본 닉네임으로 가입하고 프로필 설정에서 바꾼다
        return socialLoginService.login(SocialProvider.TOSS, tossUser.userKey(), null);
    }
}
