package com.itta.toggleme.auth.service;

import com.itta.toggleme.auth.domain.EmailVerificationPurpose;
import com.itta.toggleme.auth.exception.EmailVerificationCodeInvalidException;
import com.itta.toggleme.global.exception.BusinessException;
import com.itta.toggleme.member.domain.LocalAccount;
import com.itta.toggleme.member.domain.MemberStatus;
import com.itta.toggleme.member.repository.LocalAccountRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자체 계정의 비밀번호 재설정. 이메일로 받은 코드를 확인하면 새 비밀번호로 바꾸고 모든 기기를 로그아웃시킨다.
 * 가입 여부를 알 수 없게, 계정이 없거나 탈퇴한 이메일도 요청은 성공으로 응답하고 메일만 보내지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private final LocalAccountRepository localAccountRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public void requestCode(String rawEmail) {
        String email = LocalAccount.normalizeEmail(rawEmail);
        if (findActiveAccount(email) != null) {
            emailVerificationService.issueCode(email, EmailVerificationPurpose.PASSWORD_RESET);
        }
    }

    /** 코드가 틀렸을 때 틀린 횟수를 남겨야 해서 BusinessException이 나도 롤백하지 않는다. 비밀번호 변경은 코드 확인 뒤에만 한다. */
    @Transactional(noRollbackFor = BusinessException.class)
    public void resetPassword(String rawEmail, String code, String newPassword) {
        String email = LocalAccount.normalizeEmail(rawEmail);
        LocalAccount account = findActiveAccount(email);
        if (account == null) {
            // 계정이 없는 이메일도 코드가 틀린 것과 같은 응답을 준다
            throw new EmailVerificationCodeInvalidException();
        }

        emailVerificationService.verifyCode(email, code, EmailVerificationPurpose.PASSWORD_RESET);

        account.changePasswordHash(passwordEncoder.encode(newPassword));
        account.markEmailVerified(Instant.now());
        tokenService.revokeAll(account.getMember().getId());
    }

    private LocalAccount findActiveAccount(String email) {
        return localAccountRepository.findWithMemberByEmail(email)
                .filter(account -> account.getMember().getStatus() == MemberStatus.ACTIVE)
                .orElse(null);
    }
}
