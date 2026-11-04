package com.itta.toggleme.member.controller;

import com.itta.toggleme.member.dto.OnboardingRequest;
import com.itta.toggleme.member.dto.OnboardingResponse;
import com.itta.toggleme.member.dto.HandleAvailabilityResponse;
import com.itta.toggleme.member.dto.MemberResponse;
import com.itta.toggleme.member.dto.MemberProfileUpdateRequest;
import com.itta.toggleme.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "회원·온보딩", description = "내 정보, 프로필 설정, 온보딩")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "고유 ID 사용 가능 여부 확인")
    @GetMapping("/handle-availability")
    public ResponseEntity<HandleAvailabilityResponse> checkHandle(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String handle
    ) {
        MemberService.HandleAvailability result = memberService.checkHandleAvailability(
                Long.valueOf(jwt.getSubject()), handle);
        return ResponseEntity.ok(new HandleAvailabilityResponse(result.available(), result.reason()));
    }

    @Operation(summary = "내 정보 조회 (온보딩 단계 포함)")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(MemberResponse.from(
                memberService.getMember(Long.valueOf(jwt.getSubject()))));
    }

    @Operation(summary = "프로필 설정 (닉네임·고유 ID)")
    @PatchMapping("/me/profile")
    public ResponseEntity<MemberResponse> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody MemberProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(MemberResponse.from(memberService.updateProfile(
                Long.valueOf(jwt.getSubject()), request.handle(), request.nickname())));
    }

    @Operation(summary = "온보딩 가구 선택·방 생성")
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
