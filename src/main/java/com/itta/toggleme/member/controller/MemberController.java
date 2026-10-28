package com.itta.toggleme.member.controller;

import com.itta.toggleme.member.dto.OnboardingRequest;
import com.itta.toggleme.member.dto.OnboardingResponse;
import com.itta.toggleme.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingResponse> completeOnboarding(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OnboardingRequest request
    ) {
        Long roomId = memberService.completeOnboarding(Long.valueOf(jwt.getSubject()), request.furnitureVariantIds());
        return ResponseEntity.ok(new OnboardingResponse(roomId));
    }
}
