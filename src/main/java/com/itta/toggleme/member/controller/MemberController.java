package com.itta.toggleme.member.controller;

import com.itta.toggleme.member.dto.OnboardingRequest;
import com.itta.toggleme.member.dto.OnboardingResponse;
import com.itta.toggleme.member.dto.HandleAvailabilityResponse;
import com.itta.toggleme.member.dto.MemberProfileResponse;
import com.itta.toggleme.member.dto.MemberProfileUpdateRequest;
import com.itta.toggleme.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/handle-availability")
    public ResponseEntity<HandleAvailabilityResponse> checkHandle(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String handle
    ) {
        MemberService.HandleAvailability result = memberService.checkHandleAvailability(
                Long.valueOf(jwt.getSubject()), handle);
        return ResponseEntity.ok(new HandleAvailabilityResponse(result.available(), result.reason()));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<MemberProfileResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(MemberProfileResponse.from(memberService.updateProfile(
                Long.valueOf(jwt.getSubject()), request.handle(), request.nickname())));
    }

    @PostMapping("/onboarding")
    public ResponseEntity<OnboardingResponse> completeOnboarding(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OnboardingRequest request
    ) {
        Long roomId = memberService.completeOnboarding(
                Long.valueOf(jwt.getSubject()),
                request.furnitureVariantIds(),
                request.visibility()
        );
        return ResponseEntity.ok(new OnboardingResponse(roomId));
    }
}
