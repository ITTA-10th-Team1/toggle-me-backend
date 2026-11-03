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

    // 고유 ID(20자)·닉네임(30자) 중 긴 쪽
    private static final int MAX_LENGTH = 30;
    private static final int MAX_RESULTS = 20;

    private final MemberSearchRepository memberSearchRepository;

    // 고유 ID 또는 닉네임이 입력값으로 시작하는 회원. 고유 ID는 소문자로 저장되고, 닉네임 비교는 DB가 대소문자를 무시한다
    public List<MemberSearchResponse> search(Long memberId, String keyword) {
        String text = keyword.strip().toLowerCase(Locale.ROOT);
        if (text.isEmpty() || text.length() > MAX_LENGTH) {
            return List.of();
        }
        // %, _ 는 LIKE에서 와일드카드라 '!'를 붙여 글자 그대로 찾는다
        String prefix = text.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return memberSearchRepository.searchByPrefix(memberId, prefix, Limit.of(MAX_RESULTS));
    }
}
