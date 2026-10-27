package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.exception.CredentialsInvalidException;
import com.itta.toggleme.auth.exception.EmailAlreadyExistsException;
import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.repository.LocalAccountRepository;
import com.itta.toggleme.member.service.MemberService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalAuthService {

    private static final String MISSING_ACCOUNT_HASH = "$2a$10$5pX4fhb9fdyZLoo9JxLWWuIcgF.bhi6j3WD3ibNpr4CYfiWX1bL12";

    private final LocalAccountRepository localAccountRepository;
    private final MemberService memberService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse signUp(String email, String password, String nickname) {
        String normalizedEmail = LocalAccount.normalizeEmail(email);
        if (localAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        Member member = memberService.register(nickname);
        saveAccount(member, normalizedEmail, passwordEncoder.encode(password));
        return LoginResponse.of(tokenService.issue(member), !member.isOnboardingCompleted());
    }

    @Transactional
    public LoginResponse login(String email, String password) {
        Optional<LocalAccount> found = localAccountRepository.findWithMemberByEmail(LocalAccount.normalizeEmail(email));
        // 계정이 없어도 해시 비교를 수행해서 응답 시간으로 가입 여부를 알 수 없게 한다
        String storedHash = found.map(LocalAccount::getPasswordHash).orElse(MISSING_ACCOUNT_HASH);
        boolean passwordMatches = passwordEncoder.matches(password, storedHash);

        Member member = found
                .filter(account -> passwordMatches)
                .map(LocalAccount::getMember)
                .filter(foundMember -> foundMember.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(CredentialsInvalidException::new);
        return LoginResponse.of(tokenService.issue(member), !member.isOnboardingCompleted());
    }

    private void saveAccount(Member member, String email, String passwordHash) {
        try {
            localAccountRepository.saveAndFlush(LocalAccount.create(member, email, passwordHash));
        } catch (DataIntegrityViolationException e) {
            // 중복 확인과 저장 사이에 같은 이메일이 먼저 가입된 경우
            throw new EmailAlreadyExistsException();
        }
    }
}
