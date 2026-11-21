package com.itta.toggleme.item.dto;

import java.util.List;

// tags는 유형명을 만드는 데 쓴 태그 이름(# 없이, 많이 나온 순)이다
public record TasteProfileSummaryResponse(String typeName, String description, List<String> tags) {
}
