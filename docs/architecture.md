# Architecture

## 시스템 구성

| 구성 요소 | 역할 |
| --- | --- |
| Flutter 앱 | iOS·Android 클라이언트 |
| Spring Boot API | 단일 애플리케이션 |
| MySQL | 주 저장소, 스키마는 Flyway로 관리 |
| 카카오 API | 로그인 시 카카오 access token 검증과 회원번호 조회 |
| Amazon S3 | 이미지 원본 저장, 클라이언트에는 URL로 전달 |

## 도메인 경계

기본 패키지는 `com.itta.toggleme`. 도메인 중심으로 나누고, 각 도메인 안을 계층별로 나눈다.

```plain text
global/
├── config/
├── exception/
├── response/
└── entity/        // BaseTimeEntity

auth/
image/             // S3 업로드, presigned URL

member/
├── controller/
├── service/
├── repository/
├── domain/
└── dto/

room/
├── controller/
├── service/
├── repository/
├── domain/
└── dto/

item/
├── controller/
├── service/
├── repository/
├── domain/
└── dto/
```

- 패키지명은 영문 소문자 단수형을 쓴다 (`item` ○ / `items` ✕)
- 여러 단어가 필요하면 붙여 쓰고 언더스코어·하이픈은 쓰지 않는다

## 의존 방향

```plain text
Controller → Service → Repository
```

- 의존은 한 방향으로만 흐른다. 계층별 책임은 [layer-boundaries.md](layer-boundaries.md)
- MVP에서는 Facade와 UseCase 계층을 만들지 않는다. 여러 도메인과 외부 시스템을 조율해야 할 때 추가한다

## 인증 흐름

1. 앱이 카카오 SDK로 받은 카카오 access token을 `POST /api/v1/auth/oauth/kakao`로 보낸다
2. 서버가 카카오 토큰 정보 API로 `app_id`가 우리 앱인지 확인하고, 사용자 정보 API로 카카오 회원번호를 얻는다
3. 소셜 계정(`provider`, `provider_user_id`)으로 회원을 찾고, 없으면 새로 만든다
4. 서비스 access token(JWT)과 refresh token을 발급한다. refresh token은 해시로만 저장한다
