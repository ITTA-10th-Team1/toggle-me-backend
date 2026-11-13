package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.member.domain.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 소셜 로그인 공통 처리. 같은 계정의 첫 로그인이 동시에 들어와 DB unique 제약에 걸리면(한쪽 트랜잭션이 롤백된다)
 * 한 번 더 시도한다. 두 번째는 새 트랜잭션이라 먼저 만들어진 회원을 찾아 로그인한다.
 * 이 클래스에는 트랜잭션을 걸지 않는다. 걸면 롤백된 트랜잭션 안에서 재시도하게 된다.
 */
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final SocialLoginProcessor socialLoginProcessor;

    public LoginResponse login(SocialProvider provider, String providerUserId, String nickname) {
        try {
            return socialLoginProcessor.loginOrRegister(provider, providerUserId, nickname);
        } catch (DataIntegrityViolationException e) {
            return socialLoginProcessor.loginOrRegister(provider, providerUserId, nickname);
        }
    }
}
