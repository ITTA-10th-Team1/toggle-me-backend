package com.itta.toggleme.image.controller;

import com.itta.toggleme.image.dto.ImageUploadUrlCreateRequest;
import com.itta.toggleme.image.dto.ImageUploadUrlResponse;
import com.itta.toggleme.image.service.ImageService;
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
@RequestMapping("/api/v1/images")
public class ImageController {

    private final ImageService imageService;

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
}
