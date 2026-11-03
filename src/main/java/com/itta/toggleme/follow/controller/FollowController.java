package com.itta.toggleme.follow.controller;

import com.itta.toggleme.follow.dto.FollowMemberResponse;
import com.itta.toggleme.follow.dto.MemberProfileResponse;
import com.itta.toggleme.follow.service.FollowService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 회원은 고유 ID(handle)로 찾는다. /me 같은 고정 경로가 먼저 매칭되고, handle은 3자 이상이라 겹치지 않는다
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members/{handle}")
public class FollowController {

    private final FollowService followService;

    @GetMapping
    public ResponseEntity<MemberProfileResponse> getProfile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String handle
    ) {
        return ResponseEntity.ok(followService.getProfile(memberId(jwt), handle));
    }

    // 이미 팔로우 중이어도 204
    @PutMapping("/follow")
    public ResponseEntity<Void> follow(@AuthenticationPrincipal Jwt jwt, @PathVariable String handle) {
        followService.follow(memberId(jwt), handle);
        return ResponseEntity.noContent().build();
    }

    // 팔로우 중이 아니어도 204
    @DeleteMapping("/follow")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal Jwt jwt, @PathVariable String handle) {
        followService.unfollow(memberId(jwt), handle);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/followers")
    public ResponseEntity<List<FollowMemberResponse>> getFollowers(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String handle
    ) {
        return ResponseEntity.ok(followService.getFollowers(memberId(jwt), handle));
    }

    @GetMapping("/following")
    public ResponseEntity<List<FollowMemberResponse>> getFollowing(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String handle
    ) {
        return ResponseEntity.ok(followService.getFollowing(memberId(jwt), handle));
    }

    private Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
