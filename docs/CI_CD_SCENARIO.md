# ShowMeTheMoney 2차 프로젝트 CI/CD 시나리오

## 1. 목적과 기준

이 문서는 1단계 필수 아키텍처를 기준으로 GitHub Actions와 Helm을 사용해 애플리케이션을 Amazon EKS에 무중단 배포하기 위한 구현 시나리오다.

- CI/CD 담당: GitHub Actions 구성
- 배포 대상: Amazon EKS Auto Mode
- 이미지 저장소: Amazon ECR
- 배포 도구: Helm
- 배포 방식: Kubernetes Rolling Update
- 신규 서비스 도메인: `smtm.mang.pe.kr`
- 기존 `team4.mang.pe.kr` 서비스는 단계별 전환 동안 유지한다.
- EKS 신규 리소스는 인프라팀 기준에 따라 `Version=smtm-v1` 태그를 사용한다.

아키텍처 파일의 Single EC2 페이지는 기존 구조 참고용으로 보고, 이 CI/CD의 배포 대상은 `[REQ]`로 표시된 EKS Runtime과 GitHub OIDC 구성으로 정한다. 아키텍처의 예시 도메인과 `kubectl apply` 표기는 최신 팀 결정에 따라 각각 `smtm.mang.pe.kr`와 Helm 배포로 적용한다.

문서에서 사용하는 세 가지 값은 서로 다르다.

- `Version=smtm-v1`: AWS 리소스를 구분하는 태그
- `<GIT_SHA>`: 배포할 Docker 이미지를 구분하는 태그
- Helm Revision: 배포와 롤백에 사용하는 Helm 이력 번호

현재 저장소에는 GitHub Actions와 Helm 차트가 아직 병합되지 않았다. 따라서 이 문서는 **구축 완료 문서가 아니라 팀 검토 후 구현할 시나리오**다.

가장 중요한 원칙은 다음과 같다.

> CI/CD는 백엔드 팀원이 구현한 실행 방식, 환경변수, DB 마이그레이션을 임의로 변경하지 않는다.

## 2. 목표 구조

```text
개발자 Pull Request
        │
        ▼
GitHub Actions CI
  ├─ Backend 테스트
  ├─ Frontend lint/build
  └─ Docker 이미지 빌드 확인
        │
        ▼ main 병합
GitHub Actions CD
  ├─ GitHub OIDC로 AWS 권한 획득
  ├─ Backend/Frontend 이미지 빌드
  ├─ Amazon ECR Push
  └─ Helm으로 Amazon EKS 배포
        │
        ▼
Kubernetes Rolling Update
        │
        ▼
ALB → Ingress
       ├─ /*     → Frontend Service → Frontend Pod 2개 이상
       └─ /api/* → Backend Service  → Backend Pod 2개 이상 → RDS MySQL
```

PR에서는 CI만 수행하며 AWS에 배포하지 않는다. `main`에 병합되고 CI가 성공한 경우에만 CD를 실행한다.

## 3. 백엔드 구현 우선 원칙

### 환경변수

현재 백엔드가 사용하는 다음 환경변수 이름을 그대로 사용한다.

```text
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
CORS_ALLOWED_ORIGINS
SPRING_PROFILES_ACTIVE
```

- CI에서는 테스트 전용 값을 사용한다.
- 운영에서는 Kubernetes Secret 또는 인프라팀이 정한 AWS 연동 방식으로 Pod에 주입한다.
- DB 비밀번호와 JWT Secret을 GitHub 저장소, Helm `values.yaml`, Actions 로그에 기록하지 않는다.
- 실제 Secret 주입 방식은 인프라팀의 EKS 구성에 맞춰 확정한다.
- 현재 `application.yml`의 DB 연결과 Flyway 설정을 CI/CD 작업에서 변경하지 않는다.
- `SPRING_PROFILES_ACTIVE`는 현재 기본값인 `local`을 임의로 바꾸지 않고 백엔드 담당자의 결정을 따른다.

### 시연용 데이터

`V3__seed_dummy_data.sql`의 테스트 계정과 더미 데이터는 프로젝트 시연용으로 유지한다.

- CI/CD 작업에서 마이그레이션 파일을 삭제하거나 수정하지 않는다.
- 시연 DB에는 가짜 데이터만 사용하고 실제 개인정보를 넣지 않는다.
- 실제 사용자를 받는 서비스로 전환할 때 테스트 계정 제거 여부를 다시 검토한다.

### Frontend API 주소

EKS 배포용 Frontend 이미지는 다음 주소를 사용한다.

```env
NEXT_PUBLIC_API_URL=https://smtm.mang.pe.kr
```

프론트 요청 경로가 이미 `/api/...`를 포함하므로 주소 뒤에 `/api`를 중복해서 붙이지 않는다.

## 4. 전체 실행 순서

1. 작업 브랜치에서 Pull Request를 생성한다.
2. GitHub Actions가 Backend, Frontend, Docker 빌드를 검사한다.
3. 모든 검사가 성공하면 리뷰 후 `main`에 병합한다.
4. `main`에서 CI를 다시 실행한다.
5. GitHub `production` Environment의 배포 승인을 받는다.
6. GitHub OIDC로 AWS 임시 권한을 받는다.
7. Backend와 Frontend 이미지를 빌드해 ECR에 Push한다.
8. Helm으로 EKS Deployment의 이미지 태그를 변경한다.
9. Kubernetes가 기존 Pod를 유지하면서 새 Pod를 순차적으로 교체한다.
10. 새 Pod의 Readiness Probe와 실제 서비스 주소를 확인한다.
11. 실패하면 Helm Rollback으로 직전 정상 릴리스로 복구한다.

## 5. CI 시나리오

CI는 Pull Request와 `main` Push에서 실행한다.

### Backend 검사

1. Java 21을 준비한다.
2. MySQL 8 Service Container를 실행한다.
3. 테스트용 `DB_*`, `JWT_*`, `CORS_ALLOWED_ORIGINS` 값을 전달한다.
4. 다음 명령을 실행한다.

```bash
cd backend
./gradlew test --no-daemon
```

Backend 테스트는 DataSource와 Flyway를 사용하므로 MySQL 없이 성공한다고 가정하지 않는다.

### Frontend 검사

1. Node.js 20을 준비한다.
2. 검증용 `NEXT_PUBLIC_API_URL`을 전달한다.
3. 다음 명령을 실행한다.

```bash
cd frontend
npm ci
npm run lint
npm run build
```

### Docker 검사

PR에서 다음 이미지가 정상적으로 빌드되는지 확인한다.

- `backend/Dockerfile`
- `frontend/Dockerfile`

PR 단계에서는 ECR Push와 EKS 배포를 하지 않는다.

## 6. CD 시나리오

### 사전 준비

인프라 담당자와 다음 항목을 확인한다.

- Backend/Frontend ECR 저장소 이름
- GitHub Actions용 AWS OIDC Role ARN
- OIDC Role의 ECR Push 및 EKS 접근 권한
- OIDC Role의 `eks:DescribeCluster` 권한과 EKS Access Entry 또는 Kubernetes RBAC 등록
- EKS 클러스터 이름과 Namespace
- Helm 차트 위치와 Release 이름
- 운영 Secret의 Pod 주입 방식
- ALB와 `smtm.mang.pe.kr` 연결 상태
- ALB의 `/api/* → Backend Service`, `/* → Frontend Service` 라우팅과 HTTPS 인증서
- AWS Load Balancer Controller의 Ingress 및 ALB 생성 상태
- ExternalDNS를 사용하는 경우 Route 53 레코드 생성 상태
- GitHub `production` Environment 승인자
- EKS Node Role의 ECR Pull 권한
- ECR 이미지 태그 변경 방지, 취약점 검사, 보관 정책

운영 Secret 주입 방식은 Kubernetes Secret, External Secrets Operator, Secrets Store CSI Driver 중 인프라팀이 선택한다. GitHub Actions에는 운영 DB와 JWT Secret을 읽는 권한을 주지 않는다.

EKS API가 Private Endpoint라면 GitHub-hosted Runner에서 직접 Helm 배포할 수 없다. 이 경우 EKS 네트워크에 접근 가능한 Self-hosted Runner를 사용하거나 ArgoCD 도입 이후 배포한다.

문서에 적혀 있다는 이유만으로 AWS 리소스가 실제 생성됐다고 판단하지 않는다.

### 운영 Secret 갱신 순서

EKS에서는 기존 EC2의 `backend.env` 파일을 사용하지 않는다. Secret은 다음 순서로 적용한다.

1. 인프라 담당자가 AWS 또는 Kubernetes의 운영 Secret을 갱신한다.
2. Kubernetes Secret에 새 값이 반영됐는지 확인한다.
3. Secret 반영에 실패하면 애플리케이션 배포를 시작하지 않는다.
4. Secret 확인 후 Helm 배포를 실행해 새 Pod가 최신 값을 읽도록 한다.
5. 이미지 변경 없이 Secret만 바뀐 경우에는 Pod를 Rolling Restart한다.

환경변수로 주입된 Secret은 실행 중인 Pod에서 자동으로 갱신되지 않으므로 새 Pod가 시작돼야 한다. 실제 Secret 이름과 동기화 확인 방법은 인프라 담당자가 선택한 주입 방식에 맞춘다.

### 이미지 생성과 ECR Push

Backend와 Frontend 이미지에 동일한 전체 Git SHA 태그를 사용한다.

```text
<backend-ecr>:<GIT_SHA>
<frontend-ecr>:<GIT_SHA>
```

`latest`만 사용하지 않는다. Git SHA를 사용하면 어떤 커밋이 배포됐는지 확인하고 같은 버전으로 되돌릴 수 있다.

### Helm 배포

ArgoCD 적용 전에는 GitHub Actions가 Helm으로 직접 배포한다.

```bash
aws eks update-kubeconfig \
  --region ap-northeast-2 \
  --name <EKS_CLUSTER_NAME>

helm upgrade --install <RELEASE_NAME> <CHART_PATH> \
  --namespace <NAMESPACE> \
  --create-namespace \
  --set backend.image.tag=<GIT_SHA> \
  --set frontend.image.tag=<GIT_SHA> \
  --atomic \
  --cleanup-on-fail \
  --wait \
  --timeout 5m
```

실제 Release 이름, 차트 경로, `values.yaml` 키는 Helm 차트를 만든 팀원의 구현을 기준으로 맞춘다.
`--atomic`은 제한 시간 내 배포가 성공하지 못하면 해당 Helm 배포를 실패 처리하고 이전 상태로 되돌린다.

## 7. Rolling Update와 헬스체크

Helm 차트의 Kubernetes Deployment는 `RollingUpdate`를 사용한다.

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxUnavailable: 0
    maxSurge: 1
```

- 무중단 배포를 위해 Backend와 Frontend의 replica를 각각 최소 2개로 구성한다.
- 새 Pod가 준비되기 전에는 기존 Pod를 종료하지 않는다.
- Readiness Probe가 성공한 Pod만 트래픽을 받는다.
- 배포 과정은 `kubectl rollout status` 또는 Helm `--wait`로 확인한다.
- PodDisruptionBudget은 정상 Pod가 최소 1개 유지되도록 설정한다.
- HPA는 아키텍처 기준인 CPU 60%, Memory 70%를 시작값으로 사용하고 실제 부하 테스트 후 조정한다.
- EKS에 새 Pod를 추가할 여유 자원을 확보한다.
- 종료 중인 Pod가 요청을 마칠 수 있도록 종료 유예 시간을 설정한다.

Backend의 DB 연결 실패 응답을 HTTP 200에서 503으로 바꾸는 작업은 EKS 배포 전에 병합 여부를 확인한다.

- DB 정상: `/api/health`가 HTTP 200을 반환
- DB 장애: `/api/health`가 HTTP 503을 반환
- `/api/health`는 DB 연결 여부가 포함되므로 Readiness Probe에 사용한다.
- Liveness Probe까지 DB 상태에 연결하면 DB 장애 때 Pod가 불필요하게 재시작될 수 있으므로 별도 경로 사용 여부를 백엔드 담당자와 확인한다.
- Frontend Readiness Probe는 `/` 또는 Helm 담당자가 정한 별도 경로를 사용한다.

## 8. 성공 판정과 롤백

다음 항목이 모두 성공해야 배포 성공이다.

1. Helm 배포가 제한 시간 안에 완료된다.
2. Backend와 Frontend Deployment가 정상 상태다.
3. 모든 새 Pod의 Readiness Probe가 성공한다.
4. Backend와 Frontend의 Ready Pod가 각각 최소 2개다.
5. HPA와 PodDisruptionBudget이 정상 적용돼 있다.
6. `https://smtm.mang.pe.kr/`에 접속할 수 있다.
7. `https://smtm.mang.pe.kr/api/health`가 HTTP 200을 반환한다.
8. 응답에서 DB 연결 상태가 정상이다.

배포 실패 시 다음 순서로 복구한다.

```bash
helm history <RELEASE_NAME> --namespace <NAMESPACE>
helm rollback <RELEASE_NAME> <LAST_GOOD_REVISION> \
  --namespace <NAMESPACE> \
  --wait \
  --timeout 5m
```

- 롤백 후 Pod 상태와 서비스 주소를 다시 검사한다.
- 롤백 명령이 실패하거나 상태 검사가 실패하면 Workflow를 실패 처리한다.
- 이때 `helm status`, Pod 상태, Kubernetes Event를 기록하고 추가 자동 배포를 중단한다.
- 자동 롤백도 실패하면 인프라 담당자가 마지막 정상 Helm Revision과 Pod 상태를 확인해 수동 복구한다.
- Flyway로 적용된 DB 변경은 Helm Rollback으로 되돌아가지 않는다.
- DB 마이그레이션 문제가 의심되면 백엔드 담당자가 먼저 확인한다.
- Rolling Update 중에는 구버전과 신버전 Pod가 함께 실행되므로 DB 변경의 상호 호환성을 백엔드 담당자가 확인한다.

## 9. 향후 ArgoCD 전환

ArgoCD 도입 전에는 GitHub Actions가 Helm으로 직접 배포하고, 도입 후에는 두 방식을 동시에 사용하지 않는다. 도입 후 GitHub Actions는 ECR Push와 GitOps 이미지 태그 변경까지만 수행하며 ArgoCD가 EKS에 배포한다. 이때 롤백은 직접 `helm rollback`하지 않고 GitOps의 이미지 태그를 이전 Git SHA로 되돌린다.

## 10. 구현 순서와 역할

### 구현 순서

1. 현재 PR에서 CI/CD 시나리오를 팀이 검토한다.
2. Backend의 DB 장애 503 응답 변경을 병합한다.
3. Helm 담당자가 차트와 Probe를 구성한다.
4. CI 담당자가 `.github/workflows/ci.yml`을 구현한다.
5. CD 담당자가 OIDC, ECR Push, Helm 배포 Workflow를 구현한다.
6. 인프라 담당자가 EKS, ALB, 도메인, Secret 연결을 확인한다.
7. 테스트 배포와 Helm Rollback을 실제로 검증한다.
8. ArgoCD가 준비되면 직접 Helm 배포를 GitOps 방식으로 전환한다.

### 역할

| 담당 | 확인 내용 |
| --- | --- |
| CI/CD 담당 | GitHub Actions, ECR 이미지 태그, 배포 결과 기록 |
| Backend 담당 | 환경변수, Flyway, 헬스체크 503 응답 |
| Helm 담당 | Chart, Deployment, Service, Probe, Rolling Update |
| 인프라 담당 | EKS, ECR, IAM/OIDC, ALB, DNS, Secret, 리소스 태그 |

## 11. 완료 기준

다음 항목을 실제로 재현한 뒤 CI/CD 구축 완료로 판단한다.

1. 오류가 있는 PR은 CI에 실패한다.
2. 정상 PR은 Backend, Frontend, Docker 검사를 통과한다.
3. `main` 병합과 승인 후 이미지가 Git SHA 태그로 ECR에 저장된다.
4. Helm 배포로 EKS Pod가 Rolling Update된다.
5. Backend 503 변경이 병합된 뒤, DB 장애 Pod가 Readiness에 실패하여 트래픽을 받지 않는다.
6. 화면, API, DB 연결이 정상 동작한다.
7. 배포 실패 시 직전 Helm Revision으로 복구된다.
8. GitHub Actions에서 배포 커밋과 성공 또는 실패 지점을 확인할 수 있다.
