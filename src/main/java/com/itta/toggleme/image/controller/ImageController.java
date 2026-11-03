package com.itta.toggleme.image.controller;

import com.itta.toggleme.image.dto.ImageAnalysisCreateRequest;
import com.itta.toggleme.image.dto.ImageAnalysisResponse;
import com.itta.toggleme.image.dto.ImageUploadUrlCreateRequest;
import com.itta.toggleme.image.dto.ImageUploadUrlResponse;
import com.itta.toggleme.image.service.ImageAnalysisService;
import com.itta.toggleme.image.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "이미지·AI 태그", description = "기록하기 ①② 사진 업로드 URL 발급, AI 추천 태그")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageService imageService;
    private final ImageAnalysisService imageAnalysisService;

    @Operation(summary = "① 사진 업로드 URL 발급 (앱이 S3에 직접 PUT)")
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
    @Operation(summary = "② AI 추천 태그 분석 요청 (202 + analysisId, 비동기)")
    @PostMapping("/analysis")
    public ResponseEntity<ImageAnalysisResponse> requestAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ImageAnalysisCreateRequest request
    ) {
        return ResponseEntity.accepted().body(imageAnalysisService.requestAnalysis(
                Long.valueOf(jwt.getSubject()),
                request.imageKey(),
                request.furnitureTypeId()
        ));
    }

    @Operation(summary = "② AI 추천 태그 결과 조회 (2초 간격, 60초 지나면 FAILED)")
    @GetMapping("/analysis/{analysisId}")
    public ResponseEntity<ImageAnalysisResponse> getAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long analysisId
    ) {
        return ResponseEntity.ok(imageAnalysisService.getAnalysis(Long.valueOf(jwt.getSubject()), analysisId));
    }
}
