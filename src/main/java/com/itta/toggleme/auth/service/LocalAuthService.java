package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.dto.LoginResponse;
import com.itta.toggleme.auth.exception.CredentialsInvalidException;
import com.itta.toggleme.auth.exception.EmailAlreadyExistsException;
import com.itta.toggleme.auth.exception.LoginIdAlreadyExistsException;
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
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_local_account_email";

    private final LocalAccountRepository localAccountRepository;
    private final MemberService memberService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse signUp(String loginId, String email, String password, String nickname) {
        String normalizedLoginId = LocalAccount.normalizeLoginId(loginId);
        String normalizedEmail = LocalAccount.normalizeEmail(email);
        if (localAccountRepository.existsByLoginId(normalizedLoginId)) {
            throw new LoginIdAlreadyExistsException();
        }
        if (localAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }

        Member member = memberService.register(nickname);
        saveAccount(member, normalizedLoginId, normalizedEmail, passwordEncoder.encode(password));
        return LoginResponse.of(tokenService.issue(member), !member.isOnboardingCompleted());
    }

    @Transactional
    public LoginResponse login(String loginId, String password) {
        Optional<LocalAccount> found = localAccountRepository.findWithMemberByLoginId(
                LocalAccount.normalizeLoginId(loginId));
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

    private void saveAccount(Member member, String loginId, String email, String passwordHash) {
        try {
            localAccountRepository.saveAndFlush(LocalAccount.create(member, loginId, email, passwordHash));
        } catch (DataIntegrityViolationException e) {
            // 중복 확인과 저장 사이에 같은 값이 먼저 저장된 경우. 어느 unique 제약인지로 구분한다
            throw isEmailConflict(e) ? new EmailAlreadyExistsException() : new LoginIdAlreadyExistsException();
        }
    }

    private boolean isEmailConflict(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains(EMAIL_UNIQUE_CONSTRAINT);
    }
}
