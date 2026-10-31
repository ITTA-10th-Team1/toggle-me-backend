package com.itta.toggleme.image.domain;

import java.util.List;
import java.util.Objects;

// image_analysis.result(JSON)에 저장하고 API로도 그대로 내보낸다
public record ImageAnalysisResult(List<String> tags) {

    private static final int MAX_TAGS = 3;
    private static final int MAX_TAG_LENGTH = 20;

    // 모델 출력이 프롬프트 제한을 넘길 때가 있어 화면 규칙(최대 3개)에 맞춰 정리한다
    public ImageAnalysisResult {
        tags = tags == null ? List.of() : tags.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(tag -> !tag.isEmpty() && !tag.equals("#"))
                .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
                .filter(tag -> tag.length() <= MAX_TAG_LENGTH)
                .distinct()
                .limit(MAX_TAGS)
                .toList();
    }
}
