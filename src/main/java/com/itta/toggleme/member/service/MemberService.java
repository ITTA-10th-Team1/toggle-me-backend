package com.itta.toggleme.member.service;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.exception.HandleGenerationFailedException;
import com.itta.toggleme.member.repository.MemberRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final String DEFAULT_NICKNAME = "토글러";
    private static final int NICKNAME_MAX_LENGTH = 30;
    private static final String HANDLE_PREFIX = "user_";
    private static final String HANDLE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int HANDLE_RANDOM_LENGTH = 8;
    private static final int HANDLE_MAX_ATTEMPTS = 5;

    private final MemberRepository memberRepository;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Member register(String nickname) {
        Member member = Member.create(generateUniqueHandle(), normalizeNickname(nickname));
        return memberRepository.save(member);
    }

    private String generateUniqueHandle() {
        for (int attempt = 0; attempt < HANDLE_MAX_ATTEMPTS; attempt++) {
            String handle = HANDLE_PREFIX + randomString(HANDLE_RANDOM_LENGTH);
            if (!memberRepository.existsByHandle(handle)) {
                return handle;
            }
        }
        throw new HandleGenerationFailedException();
    }

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(HANDLE_CHARACTERS.charAt(random.nextInt(HANDLE_CHARACTERS.length())));
        }
        return builder.toString();
    }

    private String normalizeNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_NICKNAME;
        }
        String trimmed = nickname.strip();
        return trimmed.length() > NICKNAME_MAX_LENGTH ? trimmed.substring(0, NICKNAME_MAX_LENGTH) : trimmed;
    }
}
