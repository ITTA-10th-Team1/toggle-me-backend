package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.client.KakaoClient;
import com.itta.toggleme.auth.client.KakaoUserInfo;
import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.dto.TokenResponse;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.SocialAccount;
import com.itta.toggleme.member.domain.SocialProvider;
import com.itta.toggleme.member.repository.SocialAccountRepository;
import com.itta.toggleme.member.service.MemberService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final KakaoClient kakaoClient;
    private final SocialAccountRepository socialAccountRepository;
    private final MemberService memberService;
    private final TokenService tokenService;

    @Transactional
    public LoginResponse loginWithKakao(String kakaoAccessToken) {
        KakaoUserInfo kakaoUser = kakaoClient.getUserInfo(kakaoAccessToken);

        Optional<SocialAccount> socialAccount =
                socialAccountRepository.findWithMember(SocialProvider.KAKAO, kakaoUser.kakaoId());
        boolean newMember = socialAccount.isEmpty();
        Member member = socialAccount
                .map(SocialAccount::getMember)
                .orElseGet(() -> registerKakaoMember(kakaoUser));

        TokenResponse token = tokenService.issue(member);
        return LoginResponse.of(token, newMember);
    }

    private Member registerKakaoMember(KakaoUserInfo kakaoUser) {
        Member member = memberService.register(kakaoUser.nickname());
        socialAccountRepository.save(SocialAccount.connect(member, SocialProvider.KAKAO, kakaoUser.kakaoId()));
        return member;
    }
}
