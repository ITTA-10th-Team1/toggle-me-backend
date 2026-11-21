package com.itta.toggleme.item.service;

// AI가 만든 취향 유형명과 한 줄 설명. 모델 출력이 규칙을 넘길 때가 있어 서버가 공백을 정리하고 service가 길이를 확인한다
public record TasteProfileSummary(String typeName, String description) {

    public TasteProfileSummary {
        typeName = typeName == null ? "" : typeName.strip();
        description = description == null ? "" : description.strip();
    }
}
