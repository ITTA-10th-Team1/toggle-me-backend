package com.itta.toggleme.item.client;

// 네이버 검색 호출 자체가 실패했다(인증 오류, 서버 장애, 시간 초과 등). 검색 결과가 없는 것과 구분한다
public class NaverSearchException extends RuntimeException {

    public NaverSearchException(Throwable cause) {
        super(cause.getClass().getSimpleName(), cause);
    }
}
