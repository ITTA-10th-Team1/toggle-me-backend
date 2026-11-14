package com.itta.toggleme.item.client;

// 카카오 책 검색 호출 자체가 실패했다(인증 오류, 서버 장애, 시간 초과 등). 검색 결과가 없는 것과 구분한다
public class KakaoBookSearchException extends RuntimeException {

    public KakaoBookSearchException(Throwable cause) {
        super(cause.getClass().getSimpleName(), cause);
    }
}
