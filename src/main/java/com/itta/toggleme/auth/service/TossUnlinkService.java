package com.itta.toggleme.auth.service;

import com.itta.toggleme.member.domain.SocialProvider;
import com.itta.toggleme.member.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TossUnlinkService {

    private final SocialAccountRepository socialAccountRepository;
    private final TokenService tokenService;

    /**
     * 사용자가 토스에서 연결을 끊거나 탈퇴했을 때 해당 회원을 로그아웃시킨다(Refresh Token 전부 폐기).
     * 회원 데이터는 지우지 않는다. 모르는 userKey도 같은 결과로 처리해서 가입 여부를 알 수 없게 한다.
     */
    @Transactional
    public void handleUnlink(String userKey) {
        socialAccountRepository.findWithMember(SocialProvider.TOSS, userKey)
                .ifPresent(account -> tokenService.revokeAll(account.getMember().getId()));
    }
}
