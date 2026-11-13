package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.SocialAccount;
import com.itta.toggleme.member.domain.SocialProvider;
import com.itta.toggleme.member.repository.SocialAccountRepository;
import com.itta.toggleme.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 계정으로 회원을 찾거나 가입시키고 토큰을 발급한다. 한 번의 호출이 한 트랜잭션이다.
 * 동시 가입 충돌의 재시도는 트랜잭션 밖의 {@link SocialLoginService}가 맡는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginProcessor {

    private final SocialAccountRepository socialAccountRepository;
    private final MemberService memberService;
    private final TokenService tokenService;

    @Transactional
    public LoginResponse loginOrRegister(SocialProvider provider, String providerUserId, String nickname) {
        Member member = socialAccountRepository.findWithMember(provider, providerUserId)
                .map(SocialAccount::getMember)
                .orElseGet(() -> register(provider, providerUserId, nickname));
        return LoginResponse.of(tokenService.issue(member), member);
    }

    private Member register(SocialProvider provider, String providerUserId, String nickname) {
        Member member = memberService.register(nickname);
        // 동시에 같은 계정이 가입하면 이 시점(flush)에 unique 위반이 나도록 바로 반영한다
        socialAccountRepository.saveAndFlush(SocialAccount.connect(member, provider, providerUserId));
        return member;
    }
}
