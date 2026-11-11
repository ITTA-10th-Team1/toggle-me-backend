package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.client.TossClient;
import com.itta.toggleme.auth.client.TossUserInfo;
import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.dto.TokenResponse;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.SocialAccount;
import com.itta.toggleme.member.domain.SocialProvider;
import com.itta.toggleme.member.repository.SocialAccountRepository;
import com.itta.toggleme.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TossAuthService {

    private final TossClient tossClient;
    private final SocialAccountRepository socialAccountRepository;
    private final MemberService memberService;
    private final TokenService tokenService;

    @Transactional
    public LoginResponse loginWithToss(String authorizationCode, String referrer) {
        TossUserInfo tossUser = tossClient.getUserInfo(authorizationCode, referrer);

        Member member = socialAccountRepository.findWithMember(SocialProvider.TOSS, tossUser.userKey())
                .map(SocialAccount::getMember)
                .orElseGet(() -> registerTossMember(tossUser));

        TokenResponse token = tokenService.issue(member);
        return LoginResponse.of(token, member);
    }

    private Member registerTossMember(TossUserInfo tossUser) {
        // userKey 범위만 받아서 닉네임이 없다. 기본 닉네임으로 가입하고 프로필 설정에서 바꾼다
        Member member = memberService.register(null);
        socialAccountRepository.save(SocialAccount.connect(member, SocialProvider.TOSS, tossUser.userKey()));
        return member;
    }
}
