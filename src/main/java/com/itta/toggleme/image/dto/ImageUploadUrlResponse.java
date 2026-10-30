package com.itta.toggleme.image.dto;

import java.time.Instant;
import java.util.Map;

// 앱은 uploadUrl로 PUT 요청을 보내면서 headers를 그대로 붙인다. 값이 다르면 S3가 서명 오류로 거절한다
public record ImageUploadUrlResponse(
        String uploadUrl,
        String imageKey,
        Map<String, String> headers,
        Instant expiresAt
) {
}
