# API Docs

프론트엔드-백엔드 간 API 스펙 문서 모음. 1차 프로젝트에서 프론트/백엔드 API 계약이 어긋나 통합 이슈가 있었던 만큼, 구현 전에 이 문서로 스펙을 합의하고 진행한다.

- `api.md` — 엔드포인트별 요청/응답 스펙 (사람이 읽는 문서)
- `api.postman_collection.json` — 위 스펙을 검증/테스트하기 위한 Postman 컬렉션. Postman에 Import해서 사용.

두 파일은 항상 같은 내용을 반영하도록 함께 업데이트한다.

## Postman 컬렉션 사용법

1. Postman에서 `api.postman_collection.json`을 Import.
2. 컬렉션 변수 `baseUrl` 확인. 기본값은 로컬 백엔드(`http://localhost:8080/api`) 기준이며, 배포 단계(dev/staging/prod)에 맞는 서버 주소로 직접 바꿔서 사용한다. 환경별 실제 주소가 정해지면 이 README와 컬렉션 기본값도 업데이트할 것.
3. `Auth > 로그인` 요청을 실행하면 응답의 `accessToken`이 test 스크립트에 의해 컬렉션 변수에 자동 저장된다. 이후 요청들은 별도 설정 없이 이 토큰으로 인증된다.
4. `transactionId` / `recurringItemId` / `budgetId` 변수는 자동으로 채워지지 않는다. 단건 조회·수정·삭제 요청을 테스트하려면 실제 생성한 리소스의 id로 직접 변경해야 한다.
