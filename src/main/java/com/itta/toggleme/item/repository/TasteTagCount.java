package com.itta.toggleme.item.repository;

// 취향 카드 집계 한 줄: 태그 이름(# 없이)과 그 태그가 붙은 기록 수
public record TasteTagCount(String name, long count) {
}
