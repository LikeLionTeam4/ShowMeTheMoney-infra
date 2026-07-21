# ShowMeTheMoney-infra

## 프로젝트 개요
1차 프로젝트에서 구축한 개인 자산 관리 플랫폼 "Show Me The Money"의 인프라를 AWS 기반으로 전면 전환하는 2차 프로젝트입니다.

- 1차 프로젝트: https://github.com/YeonWoojuice/ShowMeTheMoney/tree/main

- **인프라 전환**: VMware Fusion 기반 온프레미스 Kubernetes → AWS EKS
- **데이터베이스**: RDS MySQL 적용으로 백업·복구 체계 강화
- **IaC**: Terraform 기반으로 인프라 재현성 확보
- **CI/CD**: GitHub Actions + ECR + Helm으로 이미지 빌드·패키징 자동화, ArgoCD 기반 GitOps 배포
- **모니터링**: CloudWatch 도입으로 운영 안정성 강화
- **서비스 기능**: 1차 MVP(회원가입/로그인, 수입·지출 CRUD, 예산, 대시보드 등)를 유지하되, 1차에서 미흡했던 프론트-백엔드 API 스펙 사전 합의로 통합 안정성 개선

단순 가계부 MVP를 넘어 운영 가능한 클라우드 기반 금융 관리 서비스로 확장하는 것이 목표입니다.

## 진행 상태
- 위 방향성에는 합의된 상태이며, 현재는 폴더 구조 초안만 잡아둔 단계입니다.
- 세부 구현(k8s manifest 작성, Terraform 코드, CI/CD 파이프라인 구체화 등)은 팀 논의 후 진행 예정입니다.

## 폴더 구조
- `frontend/`, `backend/` — 추후 애플리케이션 코드 합류 예정
- `docs/` — 아키텍처 문서, 운영 가이드 (추후 작성)
