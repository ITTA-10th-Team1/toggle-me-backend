package com.itta.toggleme.image.controller;

import com.itta.toggleme.image.dto.ImageAnalysisCreateRequest;
import com.itta.toggleme.image.dto.ImageAnalysisResponse;
import com.itta.toggleme.image.dto.ImageUploadUrlCreateRequest;
import com.itta.toggleme.image.dto.ImageUploadUrlResponse;
import com.itta.toggleme.image.service.ImageAnalysisService;
import com.itta.toggleme.image.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageService imageService;
    private final ImageAnalysisService imageAnalysisService;

    @PostMapping("/upload-url")
    public ResponseEntity<ImageUploadUrlResponse> createUploadUrl(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ImageUploadUrlCreateRequest request
    ) {
        return ResponseEntity.ok(imageService.createUploadUrl(
                Long.valueOf(jwt.getSubject()),
                request.contentType(),
                request.contentLength()
        ));
    }

    // 분석은 비동기로 처리한다. 202와 analysisId를 받은 앱은 GET으로 status가 PENDING이 아닐 때까지 조회한다
    @PostMapping("/analysis")
    public ResponseEntity<ImageAnalysisResponse> requestAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ImageAnalysisCreateRequest request
    ) {
        return ResponseEntity.accepted().body(imageAnalysisService.requestAnalysis(
                Long.valueOf(jwt.getSubject()),
                request.imageKey()
        ));
    }

    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<ImageAnalysisResponse> getAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long analysisId
    ) {
        return ResponseEntity.ok(imageAnalysisService.getAnalysis(Long.valueOf(jwt.getSubject()), analysisId));
    }
}
