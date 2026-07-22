# ShowMeTheMoney 2차 프로젝트 CI/CD 시나리오

## 1. 목적과 기준

이 문서는 현재 프로젝트의 `EC2 + Docker Compose + RDS + Nginx` 배포 구조를 GitHub Actions로 자동화하기 위한 시나리오다.

- 기준 브랜치: `origin/main`
- 기준 커밋: `5c19e49`
- 현재 `main`에는 backend, frontend, Dockerfile, Docker Compose, RDS 연동 코드가 병합되어 있다.
- 아직 GitHub Actions와 운영 배포 파일은 구현되지 않았다.
- 따라서 이번 문서는 **CI/CD 구축 완료 문서가 아니라 구현 전에 팀이 검토할 시나리오 문서**다.

가장 중요한 원칙은 다음과 같다.

> CI/CD가 백엔드 구조를 임의로 바꾸지 않고, 백엔드 팀원이 구현한 실행 방식과 환경변수를 그대로 사용한다.

장기 목표인 EKS, Helm, ArgoCD는 현재 범위에 포함하지 않는다.

## 2. 현재 프로젝트 구조

```text
사용자
  │ HTTPS
  ▼
EC2 Nginx
  ├─ /api/* → backend 컨테이너:8080
  └─ /*     → frontend 컨테이너:3000

backend 컨테이너 → RDS MySQL
```

- EC2에는 Nginx와 frontend/backend 컨테이너가 실행된다.
- RDS는 Docker Compose 외부에 있다.
- Nginx 설정은 EC2 호스트에 있다.
- 운영 Compose는 3000/8080 포트를 `127.0.0.1`에만 연결한다.
- AWS Region은 `ap-northeast-2`를 사용한다.

## 3. 백엔드 구현 우선 원칙

CI/CD는 다음 백엔드 구현을 그대로 따른다.

### 환경변수

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
CORS_ALLOWED_ORIGINS
```

- CI에서는 위 이름으로 테스트 값을 전달한다.
- 운영에서는 EC2가 Secrets Manager에서 값을 조회해 Backend 컨테이너에 전달한다.
- CI/CD 시나리오에서 새로운 환경변수 이름을 임의로 추가하지 않는다.
- 현재 `application.yml`의 DB 연결과 Flyway 설정을 CI/CD 작업에서 변경하지 않는다.
- `SPRING_PROFILES_ACTIVE`는 현재 백엔드 기본값인 `local`을 사용한다. 백엔드 팀이 운영 프로필을 추가하면 그 설정을 따른다.

### 시연용 데이터

백엔드의 `V3__seed_dummy_data.sql`에 정의된 테스트 계정과 더미 데이터는 프로젝트 시연을 위한 데이터로 보고 그대로 사용한다.

- CI/CD가 V3 파일을 삭제하거나 수정하지 않는다.
- 시연 DB에는 가짜 데이터만 사용하고 실제 개인정보를 넣지 않는다.
- 테스트 계정은 시연 용도로만 사용한다.
- 향후 실제 사용자를 받는 공개 서비스로 전환할 때 팀 합의로 테스트 계정 제거 또는 비활성화를 검토한다.
- 이미 실행된 Flyway 마이그레이션 파일은 직접 수정하지 않는다.

### Frontend API 주소

`NEXT_PUBLIC_API_URL`은 다음처럼 `/api`가 없는 서비스 도메인만 사용한다.

```env
NEXT_PUBLIC_API_URL=https://team4.mang.pe.kr
```

프론트 호출 경로가 이미 `/api/...`를 포함하므로 URL에 `/api`를 추가하면 `/api/api/...`가 된다.

## 4. 전체 CI/CD 흐름

```text
1. 작업 브랜치에서 Pull Request 생성
2. GitHub Actions CI 실행
3. Backend 테스트
4. Frontend lint/build
5. Backend/Frontend Docker 이미지 빌드 확인
6. 모든 검사가 성공하면 main 병합
7. main에서 CI 재검증
8. production 배포 승인
9. GitHub OIDC로 AWS 임시 권한 획득
10. Backend/Frontend 이미지를 ECR에 Push
11. SSM으로 EC2 배포 스크립트 실행
12. EC2가 새 이미지를 Pull하고 Docker Compose 실행
13. Frontend, API, RDS 연결 상태 확인
14. 실패하면 마지막 정상 이미지로 롤백
```

PR에서는 테스트와 이미지 빌드 확인만 하며 AWS에 배포하지 않는다.

## 5. CI 시나리오

### Backend

1. Java 21을 준비한다.
2. MySQL 8 Service Container를 실행한다.
3. 테스트용 `DB_*`, `JWT_*`, `CORS_ALLOWED_ORIGINS` 값을 전달한다.
4. `backend/`에서 다음 명령을 실행한다.

```bash
./gradlew test --no-daemon
```

`@SpringBootTest`가 DataSource와 Flyway를 사용하므로 MySQL 없이 테스트가 성공한다고 가정하지 않는다.

### Frontend

1. Node.js 20을 준비한다.
2. `frontend/`에서 다음 명령을 실행한다.

```bash
npm ci
npm run lint
npm run build
```

빌드할 때 검증용 `NEXT_PUBLIC_API_URL`을 전달한다.

### Docker

PR에서 다음 두 Dockerfile의 이미지 빌드 성공 여부를 확인한다.

- `backend/Dockerfile`
- `frontend/Dockerfile`

PR 단계에서는 ECR Push와 EC2 배포를 하지 않는다.

## 6. CD 시나리오

### 사전 확인

인프라 담당자가 실제 AWS에서 다음 항목을 확인한다.

- `team4-backend`, `team4-frontend` ECR 저장소
- GitHub Actions용 AWS OIDC Role
- EC2 Runtime Role의 ECR Pull, Secrets Manager 조회, SSM 권한
- EC2가 SSM Managed Node에서 Online 상태인지
- 운영 Secret의 실제 이름 또는 ARN

문서나 PDF에 작성되어 있다는 이유만으로 AWS 구성이 완료됐다고 판단하지 않는다.

### 최초 1회 EC2 준비

인프라 담당자가 EC2의 `/opt/team4`에 다음 파일을 설치한다.

```text
/opt/team4/docker-compose.prod.yml
/opt/team4/deploy.sh
/opt/team4/rollback.sh
```

이 준비가 끝난 뒤 GitHub Actions가 SSM으로 배포 스크립트를 실행한다.

### 이미지 생성

1. `main`의 CI가 성공한다.
2. GitHub `production` Environment 승인을 받는다.
3. GitHub OIDC로 AWS 임시 권한을 받는다.
4. Backend와 Frontend 이미지를 빌드한다.
5. 두 이미지에 동일한 전체 Git SHA 태그를 사용한다.
6. 이미지를 ECR에 Push한다.

```text
team4-backend:<GIT_SHA>
team4-frontend:<GIT_SHA>
```

`latest`만 사용하지 않는다.

첫 구현은 AWS Role과 승인 흐름을 단순하게 유지하기 위해 **승인 후 ECR Push와 EC2 배포를 한 배포 Job에서 실행**한다.

### EC2 배포

1. GitHub Actions가 SSM Run Command를 호출한다.
2. EC2가 자신의 IAM Role로 ECR에 로그인한다.
3. EC2가 Secrets Manager에서 Backend 환경변수를 조회한다.
4. Secret을 `/opt/team4/backend.env`에 기록하고 권한을 `600`으로 제한한다.
5. 새 이미지 태그를 `/opt/team4/candidate.env`에 기록한다.
6. 운영 Compose로 이미지를 Pull한다.
7. 새 컨테이너를 실행한다.
8. 내부 주소와 외부 HTTPS 주소를 검사한다.

운영 Compose는 Backend 환경변수 파일을 명시적으로 연결한다.

```yaml
services:
  backend:
    image: "${ECR_REGISTRY}/team4-backend:${IMAGE_TAG}"
    env_file:
      - /opt/team4/backend.env
    ports:
      - "127.0.0.1:8080:8080"

  frontend:
    image: "${ECR_REGISTRY}/team4-frontend:${IMAGE_TAG}"
    ports:
      - "127.0.0.1:3000:3000"
```

Secret 값은 GitHub Actions 로그와 SSM 로그에 출력하지 않는다.

`candidate.env`와 `last-good.env`에는 다음 값만 저장한다.

```env
ECR_REGISTRY=<AWS_ACCOUNT_ID>.dkr.ecr.ap-northeast-2.amazonaws.com
IMAGE_TAG=<GIT_SHA>
```

신규 배포는 다음처럼 후보 버전 파일을 사용한다.

```bash
docker compose --env-file /opt/team4/candidate.env \
  -f /opt/team4/docker-compose.prod.yml pull

docker compose --env-file /opt/team4/candidate.env \
  -f /opt/team4/docker-compose.prod.yml up -d --remove-orphans
```

## 7. 성공 판정과 롤백

다음 항목이 모두 성공해야 배포 성공이다.

1. Backend 컨테이너가 실행 중이다.
2. Frontend 컨테이너가 실행 중이다.
3. `https://<SERVICE_DOMAIN>/` 요청이 성공한다.
4. `/api/health` 응답이 다음 조건을 만족한다.

```text
success == true
data.status == "UP"
data.db == "connected"
```

현재 Backend는 DB 연결 실패 시에도 HTTP 200을 반환하므로 HTTP 상태만 검사하면 안 된다.

```bash
curl -fsS "https://<SERVICE_DOMAIN>/api/health" \
  | jq -e '.success == true and .data.status == "UP" and .data.db == "connected"'
```

### 정상 버전 기록

```text
candidate.env = 지금 배포를 시도하는 버전
last-good.env = 마지막으로 검증에 성공한 버전
```

- 새 버전 검사 전에는 `last-good.env`를 변경하지 않는다.
- 모든 검사가 성공한 뒤에만 `candidate.env`를 `last-good.env`로 승격한다.
- 검사 실패 시 `last-good.env`의 Backend/Frontend 이미지를 다시 실행한다.
- 롤백 후 동일한 상태 검사를 다시 수행한다.
- 롤백도 실패하면 컨테이너와 로그를 보존하고 인프라 담당자가 SSM으로 수동 복구한다.

Flyway가 적용한 DB 변경은 애플리케이션 이미지 롤백으로 되돌아가지 않는다. DB 마이그레이션 문제가 의심되면 백엔드 담당자가 먼저 확인한다.

롤백은 다음처럼 마지막 정상 버전 파일을 사용한다.

```bash
docker compose --env-file /opt/team4/last-good.env \
  -f /opt/team4/docker-compose.prod.yml pull

docker compose --env-file /opt/team4/last-good.env \
  -f /opt/team4/docker-compose.prod.yml up -d --remove-orphans
```

## 8. 구현 순서와 역할

### 1단계: 현재 PR

- CI/CD 시나리오 문서 검토 및 합의
- 실제 자동화가 구현됐다고 표현하지 않음

### 2단계: CI PR

- `.github/workflows/ci.yml`
- Backend 테스트
- Frontend lint/build
- Docker 이미지 빌드 확인

### 3단계: ECR 배포 PR

- `docker-compose.prod.yml`
- GitHub OIDC와 ECR Push
- `production` Environment 승인

### 4단계: EC2 배포와 롤백 PR

- `deploy.sh`, `rollback.sh`
- SSM Run Command
- 상태 검사
- 자동 롤백

### 역할

| 담당 | 확인 내용 |
| --- | --- |
| CI/CD 담당 | 시나리오, GitHub Actions, 배포 결과 기록 |
| Backend 담당 | 현재 환경변수, Flyway, API/DB 상태 확인 |
| 인프라 담당 | ECR, IAM, OIDC, Secrets Manager, SSM, EC2 |

## 9. 완료 기준

다음이 실제로 재현되면 CI/CD 구축 완료로 본다.

1. 오류가 있는 PR은 CI 실패로 병합되지 않는다.
2. 정상 PR이 `main`에 병합되면 production 승인을 요청한다.
3. 승인 후 두 이미지가 동일한 Git SHA로 ECR에 저장된다.
4. SSM으로 EC2에 자동 배포된다.
5. 화면, API, RDS 연결이 자동 검증된다.
6. 검증 실패 시 `last-good.env` 버전으로 복구된다.
7. GitHub Actions에서 배포 SHA, SSM 결과, 성공 또는 실패 지점을 확인할 수 있다.
