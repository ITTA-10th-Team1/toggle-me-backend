# Toggle, Me! Backend

좋아하는 것들을 나만의 방에 모으는 취향 아카이브 서비스 **토글미**의 백엔드 저장소입니다.

## 기술 스택

- Java 21, Spring Boot
- Spring Security (OAuth2 Resource Server, JWT), Spring Data JPA, Flyway
- MySQL (로컬은 Docker Compose)
- springdoc-openapi (Swagger UI)

## 로컬 실행

```bash
cp .env.example .env   # DB, JWT_SECRET, KAKAO_APP_ID 값 채우기
docker compose up -d   # 로컬 MySQL
./gradlew bootRun
./gradlew test         # 로컬 MySQL이 실행 중이어야 한다
```

- API 문서: http://localhost:8080/swagger-ui/index.html

## 환경 변수

`.env.example`을 복사해 `.env`를 만든다. `.env`는 커밋하지 않는다.

| 이름 | 설명 |
| --- | --- |
| `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_ROOT_PASSWORD` | 로컬 MySQL 접속 정보 |
| `JWT_SECRET` | Base64 인코딩된 32바이트 이상 키 (`openssl rand -base64 32`) |
| `KAKAO_APP_ID` | 카카오 개발자 콘솔의 앱 ID |

## 문서

- AI 개발 도구 지침: [AGENTS.md](AGENTS.md)
- 개발 문서 목차: [docs/README.md](docs/README.md)
