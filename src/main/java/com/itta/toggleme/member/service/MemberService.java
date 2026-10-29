package com.itta.toggleme.member.service;

import com.itta.toggleme.member.domain.Member;
import com.itta.toggleme.member.dto.HandleUnavailableReason;
import com.itta.toggleme.member.exception.HandleAlreadyExistsException;
import com.itta.toggleme.member.exception.HandleInvalidException;
import com.itta.toggleme.member.exception.HandleReservedException;
import com.itta.toggleme.member.exception.HandleGenerationFailedException;
import com.itta.toggleme.member.exception.MemberNotFoundException;
import com.itta.toggleme.member.exception.MemberOnboardingAlreadyCompletedException;
import com.itta.toggleme.member.exception.MemberProfileRequiredException;
import com.itta.toggleme.member.repository.MemberRepository;
import com.itta.toggleme.room.domain.RoomVisibility;
import com.itta.toggleme.room.service.RoomService;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
    private static final int HANDLE_MIN_LENGTH = 3;
    private static final int HANDLE_MAX_LENGTH = 20;
    private static final Set<String> RESERVED_HANDLES = Set.of(
            "admin", "administrator", "root", "system", "support", "official", "toggleme", "toggle_me",
            "null", "undefined"
    );

    private final MemberRepository memberRepository;
    private final RoomService roomService;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Member register(String nickname) {
        Member member = Member.create(generateUniqueHandle(), normalizeNickname(nickname));
        return memberRepository.save(member);
    }

    @Transactional
    public Long completeOnboarding(Long memberId, List<Long> furnitureVariantIds, RoomVisibility visibility) {
        // 트랜잭션의 첫 DB 읽기여야 동시 요청이 앞선 요청이 만든 방을 본다. 앞에 일반 조회를 추가하지 말 것
        Member member = memberRepository.findByIdForUpdate(memberId).orElseThrow(MemberNotFoundException::new);
        if (!member.isProfileCompleted()) {
            throw new MemberProfileRequiredException();
        }
        Long roomId = roomService.createDefaultRoom(member, furnitureVariantIds, visibility);
        member.completeOnboarding();
        return roomId;
    }

    public Member getMember(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(MemberNotFoundException::new);
    }

    public HandleAvailability checkHandleAvailability(Long memberId, String candidate) {
        Member member = getMember(memberId);
        String handle = normalizeHandle(candidate);
        HandleUnavailableReason reason = handleReason(handle);
        if (reason != null) {
            return new HandleAvailability(false, reason);
        }
        if (handle.equals(member.getHandle())) {
            return new HandleAvailability(true, null);
        }
        boolean exists = memberRepository.existsByHandle(handle);
        return new HandleAvailability(!exists, exists ? HandleUnavailableReason.DUPLICATED : null);
    }

    @Transactional
    public Member updateProfile(Long memberId, String rawHandle, String rawNickname) {
        Member member = getMember(memberId);
        if (member.isProfileCompleted() && member.isOnboardingCompleted()) {
            throw new MemberOnboardingAlreadyCompletedException();
        }
        String handle = normalizeHandle(rawHandle);
        HandleUnavailableReason reason = handleReason(handle);
        if (reason == HandleUnavailableReason.INVALID_FORMAT) {
            throw new HandleInvalidException();
        }
        if (reason == HandleUnavailableReason.RESERVED) {
            throw new HandleReservedException();
        }
        if (!handle.equals(member.getHandle()) && memberRepository.existsByHandle(handle)) {
            throw new HandleAlreadyExistsException();
        }
        String nickname = rawNickname.strip();
        member.updateProfile(handle, nickname);
        try {
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new HandleAlreadyExistsException();
        }
        return member;
    }

    private String normalizeHandle(String candidate) {
        return candidate.strip().toLowerCase(Locale.ROOT);
    }

    private HandleUnavailableReason handleReason(String handle) {
        if (handle.length() < HANDLE_MIN_LENGTH || handle.length() > HANDLE_MAX_LENGTH
                || !handle.matches("[a-z0-9_]+")) {
            return HandleUnavailableReason.INVALID_FORMAT;
        }
        if (handle.startsWith(HANDLE_PREFIX) || RESERVED_HANDLES.contains(handle)) {
            return HandleUnavailableReason.RESERVED;
        }
        return null;
    }

    public record HandleAvailability(boolean available, HandleUnavailableReason reason) { }

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
