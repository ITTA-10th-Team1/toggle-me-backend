package com.itta.toggleme.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // global
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 경로를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // auth
    INVALID_KAKAO_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 카카오 토큰입니다."),
    KAKAO_API_ERROR(HttpStatus.BAD_GATEWAY, "카카오 서버와 통신 중 오류가 발생했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    CREDENTIALS_INVALID(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),

    // member
    HANDLE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "회원 핸들 생성에 실패했습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."),
    HANDLE_INVALID(HttpStatus.BAD_REQUEST, "핸들 형식이 올바르지 않습니다."),
    HANDLE_RESERVED(HttpStatus.BAD_REQUEST, "사용할 수 없는 핸들입니다."),
    HANDLE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 핸들입니다."),
    MEMBER_PROFILE_REQUIRED(HttpStatus.CONFLICT, "프로필 설정을 먼저 완료해야 합니다."),
    MEMBER_ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "온보딩이 완료된 회원은 프로필을 변경할 수 없습니다."),

    // room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다."),
    ROOM_STYLE_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "기본 방 스타일이 등록되어 있지 않습니다."),
    FURNITURE_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "가구 종류를 찾을 수 없습니다."),
    FURNITURE_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "가구 디자인을 찾을 수 없습니다."),
    INVALID_FURNITURE_SELECTION(HttpStatus.BAD_REQUEST, "가구 선택이 올바르지 않습니다."),
    FURNITURE_PLACEMENT_NOT_CONFIGURED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "가구의 고정 위치가 등록되어 있지 않습니다."
    ),

    // item
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "아이템을 찾을 수 없습니다."),
    ITEM_IMAGE_INVALID(HttpStatus.BAD_REQUEST, "사용할 수 없는 이미지입니다."),
    ITEM_RECORDED_DATE_INVALID(HttpStatus.BAD_REQUEST, "미래 날짜로는 기록할 수 없습니다."),
    ITEM_DAILY_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "하루에 올릴 수 있는 사진 수를 넘었습니다."),
    ITEM_FURNITURE_NOT_SELECTED(HttpStatus.FORBIDDEN, "방에 둔 가구에만 기록할 수 있습니다."),
    ITEM_TAG_INVALID(HttpStatus.BAD_REQUEST, "태그는 1~20자로 입력해 주세요."),
    FRIDGE_NOT_SELECTED(HttpStatus.FORBIDDEN, "방에 냉장고를 둔 회원만 사용할 수 있습니다.");

    private final HttpStatus status;
    private final String message;
}
