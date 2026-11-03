package com.itta.toggleme.follow.controller;

import com.itta.toggleme.follow.dto.MemberSearchResponse;
import com.itta.toggleme.follow.service.MemberSearchService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// /members/{handle}보다 고정 경로가 먼저 매칭된다. 그래서 "search"는 고유 ID로 쓸 수 없게 막아 두었다
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/search")
public class MemberSearchController {

    private final MemberSearchService memberSearchService;

    @GetMapping
    public ResponseEntity<List<MemberSearchResponse>> search(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String handle
    ) {
        return ResponseEntity.ok(memberSearchService.searchByHandle(Long.valueOf(jwt.getSubject()), handle));
    }
}
