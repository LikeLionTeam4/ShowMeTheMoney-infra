# EKS Helm 배포 전 팀원과 상의해야 하는 값

`helm/backend`, `helm/frontend` chart를 만들면서 아직 확정 안 된 값들을 플레이스홀더로 남겨뒀다. EKS/RDS를 구축 시 상의해서 아래 값들을 채운 뒤 진행한다.

## 1. AWS 계정 / 이미지

- [ ] `<ACCOUNT_ID>` — `helm/backend/values.yaml`, `helm/frontend/values.yaml`의 `image.repository`에 들어갈 실제 ECR 계정 ID
- [ ] ECR 리포지토리 이름이 `team4-backend` / `team4-frontend`로 맞는지 확인 (팀원이 다르게 만들었으면 여기도 맞춰야 함)

## 2. RDS / DB (Secret 값)

`helm/backend/values.yaml`의 `secret.data`

- [ ] `DB_HOST` — RDS 엔드포인트
- [ ] `DB_USERNAME`, `DB_PASSWORD` — RDS 계정 (기존 EC2 배포와 동일 계정 재사용할지, EKS용으로 새로 만들지)
- [ ] `JWT_SECRET` — 기존 EC2 배포와 같은 값 쓸지 결정 (다르면 기존 발급된 토큰이 EKS 쪽에서 무효화됨)
- [ ] **Secret을 클러스터에 넣는 방식** — `kubectl create secret` 수동 생성 vs External Secrets Operator + AWS Secrets Manager 연동. 값 자체보다 이 방식 결정이 우선

## 3. 도메인 / 네트워크

`helm/frontend/values.yaml`의 `ingress`

- [ ] `host` — 실제 서비스 도메인 (현재 `team4.mang.pe.kr`로 가정해둔 상태, README 예시값 그대로 가져온 것)
- [ ] `className` — Ingress Controller 종류 (현재 `alb` 가정. nginx-ingress 등 다른 걸 쓰면 전체 교체 필요)
- [ ] `tlsSecretName`, `annotations` — TLS 인증서를 ACM으로 붙일지 cert-manager로 발급할지에 따라 값이 달라짐
- [ ] `CORS_ALLOWED_ORIGINS`(backend secret) — 위 도메인과 일치해야 함

## 4. 리소스 / 스케일

- [ ] `replicaCount` (현재 backend/frontend 각 2) — 노드 그룹 크기/인스턴스 타입에 맞춰 조정
- [ ] `resources.requests/limits` (CPU/메모리) — 너무 크면 스케줄 실패, 너무 작으면 OOM. 노드 스펙 확정 후 같이 조정

## 5. 아직 chart에 없는 것

- [ ] **네임스페이스** — 현재 chart에 명시 안 돼 있어 기본(`default`)에 배포됨. 팀 전용 네임스페이스(예: `team4`) 쓸지 결정

## 참고

- `<ACCOUNT_ID>` 등 실제 계정 ID는 `docs/RESOURCE_NAMING_CONVENTION.md` 규칙에 따라 저장소/문서에 실값을 남기지 않는다.
- 값이 확정되면 `values.yaml`을 직접 고치지 말고, 실값이 들어간 `values-prod.yaml`(gitignore 대상) 등 별도 파일로 분리해서 `helm install -f`로 주입하는 걸 권장한다.
