package com.itta.toggleme.follow.service;

import com.itta.toggleme.follow.dto.MemberSearchResponse;
import com.itta.toggleme.follow.repository.MemberSearchRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberSearchService {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_RESULTS = 20;

    private final MemberSearchRepository memberSearchRepository;

    // 고유 ID 규칙(영소문자·숫자·_, 20자 이하) 밖의 입력은 찾을 회원이 없으므로 빈 목록
    public List<MemberSearchResponse> searchByHandle(Long memberId, String keyword) {
        String handle = keyword.strip().toLowerCase(Locale.ROOT);
        if (handle.length() < MIN_LENGTH || !handle.matches("[a-z0-9_]{1,20}")) {
            return List.of();
        }
        // '_'는 LIKE에서 아무 글자 한 개라 글자 그대로 찾도록 막는다. '!'는 고유 ID에 못 쓰는 글자라 이스케이프로 쓴다
        String prefix = handle.replace("_", "!_") + "%";
        return memberSearchRepository.searchByHandlePrefix(memberId, prefix, Limit.of(MAX_RESULTS));
    }
}
