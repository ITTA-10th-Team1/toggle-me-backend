# Deployment

main에 병합된 코드를 운영 서버(EC2)에 배포하는 절차다.

## 구성

| 항목 | 내용 |
| --- | --- |
| 서버 | AWS EC2 서울, t3.micro(메모리 908MB + swap 2GB), Ubuntu, 디스크 30GB |
| 도메인 | `https://api.toggleme.site` → EC2 Nginx(443) → `127.0.0.1:8080` |
| 실행 | `~/toggleme`에서 Docker Compose로 app, mysql 실행 ([docker-compose.prod.yml](../../docker-compose.prod.yml)) |
| 이미지 | `ghcr.io/itta-10th-team1/toggle-me-backend:<커밋 SHA>`, `:latest` (public 패키지) |
| 배포 | main push → [Backend CD](../../.github/workflows/backend-cd.yml): 테스트 → 이미지 push → SSH로 `compose up -d` → 헬스체크 |
| 이미지 | S3 `toggleme-prod-images`(비공개). EC2 IAM 역할 `toggleme-ec2-role`이 이 버킷의 Put·Get·Delete만 허용, 인스턴스 메타데이터 홉 제한 2 |
| 백업 | EBS 스냅샷(Data Lifecycle Manager) 하루 1회, 7일 보관. MySQL binlog는 끈다 |

배포 중 앱이 다시 뜨는 동안(수십 초) API는 502를 반환한다.

## 서버 .env

`~/toggleme/.env`에 두고 권한은 600으로 한다. 값은 서버에만 둔다.
값은 꺾쇠(`<>`)·따옴표 없이 그대로 쓴다. 예: `KAKAO_APP_ID=1234567`

| 키 | 필수 | 설명 |
| --- | --- | --- |
| `DB_NAME` | 선택 | 기본 `toggleme` |
| `DB_USERNAME`, `DB_PASSWORD` | 필수 | 앱용 DB 계정 |
| `DB_ROOT_PASSWORD` | 필수 | mysql 컨테이너만 받는다 |
| `JWT_SECRET` | 필수 | Base64 32바이트 이상, 로컬과 다른 값 |
| `KAKAO_APP_ID` | 필수 | 카카오 앱 ID |
| `JDK_JAVA_OPTIONS` | 선택 | 메모리 조정 시에만. 지정하면 compose 기본값 전체를 대체한다 |

- DB 계정·비밀번호는 mysql 볼륨을 처음 만들 때만 적용된다. 나중에 바꾸면 `ALTER USER`도 실행한다
- 서버에서 `docker compose config`를 실행하지 않는다. 비밀값이 화면에 출력된다

## GitHub 설정

| 설정 | 내용 |
| --- | --- |
| main ruleset | PR 필수, 승인 1명, force push·삭제 금지 |
| Environment `production` | 배포 브랜치 `main`만 허용 |
| Environment secrets | `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY`(배포 전용 키), `EC2_HOST_FINGERPRINT` |

GHCR 로그인은 `GITHUB_TOKEN`을 쓰므로 등록할 값이 없다.
호스트 지문은 서버 안에서 `ssh-keygen -l -E sha256 -f /etc/ssh/ssh_host_ed25519_key.pub`로 얻는다. 출력의 두 번째 값(`SHA256:...`)을 등록한다.

## 첫 배포

1. 서버: `EC2_USER`가 docker 그룹인지, `~/toggleme/.env`가 있는지, Nginx가 `X-Forwarded-Proto`·`X-Forwarded-For`·`Host`를 넘기는지 확인한다
2. 서버: `docker volume ls`에 `toggleme_mysql-data`가 없으면 첫 기동 때 Flyway가 V1부터 적용한다. 이미 있으면 `flyway_schema_history`를 먼저 확인한다
3. develop → main 릴리스 PR을 병합하면 Backend CD가 실행된다
4. 첫 실행은 GHCR 패키지가 private으로 생성되어 서버 pull이 실패한다. 패키지 설정에서 Public으로 바꾸고 "Re-run failed jobs"를 실행한다
5. 확인: 헬스체크 성공, `docker compose -f docker-compose.prod.yml logs app`의 Flyway 로그, Actions 로그에 비밀값 없음

## 메모리·디스크 점검

메모리 배분(app 640m, mysql 320m, heap 256m)은 추정치다. 첫 배포 직후, 10분 뒤, 1주 뒤에 확인한다.

```sh
free -m
docker stats --no-stream
docker inspect -f '{{.Name}} oom={{.State.OOMKilled}} restarts={{.RestartCount}}' $(docker compose -f ~/toggleme/docker-compose.prod.yml ps -q)
df -h /
docker system df
```

| 지표 | 조치 기준 |
| --- | --- |
| 컨테이너 메모리 사용 / 상한 | 상시 85% 이상 |
| OOMKilled, 재시작 횟수 | 1건이라도 발생 |
| `free -m` available | 상시 100MB 미만 |
| swap 사용량 | 상시 512MB 초과 |
| 디스크 사용률 | 70% 경고. 85% 이상이면 배포가 자동으로 중단된다 |

기준을 넘으면 `JDK_JAVA_OPTIONS`·`mem_limit`을 조정하거나 인스턴스를 키운다.

## 헬스체크 실패 시

Backend CD가 실패하면 "Show server status on failure" 단계에 컨테이너 상태와 앱 로그 100줄이 출력된다.

1. `ps -a`에서 app이 `Restarting`이거나 재시작 횟수가 늘고 있으면 앱 기동 실패다. 로그의 `APPLICATION FAILED TO START` 아래 Description을 본다
2. 설정값 오류면 서버 `.env`를 고치고 다시 만든다

   ```sh
   cd ~/toggleme
   APP_TAG=<배포할 커밋 SHA> docker compose -f docker-compose.prod.yml up -d --force-recreate app
   ```

3. 앱은 떠 있는데 502면 Nginx 설정과 `127.0.0.1:8080` 응답(`curl -s localhost:8080/actuator/health`)을 확인한다

`APP_TAG`를 빼면 `latest`로 뜬다. 배포 기록과 맞추려면 커밋 SHA를 붙인다.

## 롤백

Actions가 실패해도 서비스가 이전 버전으로 돌아간 것은 아니다. 실패 알림을 받으면 바로 확인한다.

1. 즉시: 직전 정상 커밋 SHA(Actions의 마지막 성공 실행)로 되돌린다

   ```sh
   cd ~/toggleme
   APP_TAG=<직전 정상 SHA> docker compose -f docker-compose.prod.yml up -d
   ```

2. 정식: main에 revert PR을 병합해 CD로 다시 배포한다. 1만 하고 끝내면 이후 `up -d`가 문제 버전(latest)으로 돌아간다
3. DB는 되돌리지 않는다. 스키마를 되돌려야 하면 새 마이그레이션을 추가한다 ([persistence.md](../persistence.md#마이그레이션))
