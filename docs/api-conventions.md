# API Conventions

API URL, 요청·응답, 오류, 인증 규칙이다. 개별 API 명세는 Swagger UI(`/swagger-ui/index.html`)를 기준으로 한다.

## URL

- 모든 API는 `/api/v1`로 시작한다

## 인증

- 인증이 필요한 API는 `Authorization: Bearer <access token>` 헤더를 받는다
- 사용자 식별은 access token(JWT)의 subject로만 한다
- 공개 공유 링크에는 내부 ID 대신 별도의 `shareToken`을 사용한다

## 요청·응답

- 날짜·시간은 ISO 8601 UTC 형식으로 응답한다

## 오류

- 상황을 구체적으로 표현하는 Custom Exception을 사용한다
- 모든 비즈니스 예외는 `BusinessException`을 상속한다
- `ErrorCode`에서 HTTP 상태와 사용자 메시지를 관리한다
- `GlobalExceptionHandler`에서 비즈니스 예외를 공통 `ErrorResponse`로 변환한다
- DTO 검증 실패와 예상하지 못한 서버 예외는 별도로 처리한다

```json
{
  "code": "INVALID_INPUT_VALUE",
  "message": "요청 값이 올바르지 않습니다.",
  "errors": [
    { "field": "accessToken", "reason": "카카오 Access Token은 필수입니다." }
  ]
}
```

### 예외·ErrorCode 네이밍

- 예외 클래스: `{대상}{상황}Exception` (예: `ItemNotFoundException`, `ItemAccessDeniedException`)
- ErrorCode: `{도메인}_{상황}` 형식의 UPPER_SNAKE_CASE (예: `ITEM_NOT_FOUND`, `SHARE_TOKEN_INVALID`)
- 같은 상황은 예외 클래스명과 ErrorCode가 같은 의미를 갖도록 맞춘다

```java
public class ItemNotFoundException extends BusinessException {

    public ItemNotFoundException() {
        super(ErrorCode.ITEM_NOT_FOUND);
    }
}
```
