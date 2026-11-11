package com.itta.toggleme.auth.controller;

import com.itta.toggleme.auth.client.TossCallbackAuthVerifier;
import com.itta.toggleme.auth.dto.TossUnlinkRequest;
import com.itta.toggleme.auth.service.TossUnlinkService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 앱인토스가 호출하는 연결 끊기 콜백. 앱이 부르는 API가 아니라 Swagger에서 숨긴다. */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/oauth/toss/unlink")
public class TossCallbackController {

    private final TossCallbackAuthVerifier authVerifier;
    private final TossUnlinkService tossUnlinkService;

    @GetMapping
    public ResponseEntity<Void> unlinkByGet(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam String userKey
    ) {
        authVerifier.verify(authorization);
        tossUnlinkService.handleUnlink(userKey);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Void> unlinkByPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody TossUnlinkRequest request
    ) {
        authVerifier.verify(authorization);
        tossUnlinkService.handleUnlink(request.userKey());
        return ResponseEntity.ok().build();
    }
}
