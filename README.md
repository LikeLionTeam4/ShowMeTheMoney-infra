# ShowMeTheMoney-infra

금융 데이터 관리 개인 가계 대시보드의 AWS 인프라 구축 프로젝트 (2차 프로젝트).
1차 프로젝트(VMware Fusion 기반)를 AWS/EKS 기반으로 재구성한다.

## 진행 상태
- 폴더 구조 초안만 잡은 상태입니다.
- 세부 아키텍처, k8s manifest 구성, CI/CD 방식 등은 팀 논의 후 확정 예정입니다.

## 폴더 구조
- `frontend/`, `backend/` — 추후 애플리케이션 코드 합류 예정
- `infra/k8s/` — EKS 클러스터용 Kubernetes manifest
- `docs/` — 아키텍처 문서, 운영 가이드 (추후 작성)
