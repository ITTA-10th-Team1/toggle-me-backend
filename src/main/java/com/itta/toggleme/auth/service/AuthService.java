package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.client.KakaoClient;
import com.itta.toggleme.auth.client.KakaoUserInfo;
import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.member.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 카카오 API 호출은 트랜잭션 밖에서 하고, 가입·로그인은 {@link SocialLoginService}에 맡긴다. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoClient kakaoClient;
    private final SocialLoginService socialLoginService;

    public LoginResponse loginWithKakao(String kakaoAccessToken) {
        KakaoUserInfo kakaoUser = kakaoClient.getUserInfo(kakaoAccessToken);
        return socialLoginService.login(SocialProvider.KAKAO, kakaoUser.kakaoId(), kakaoUser.nickname());
    }
}
