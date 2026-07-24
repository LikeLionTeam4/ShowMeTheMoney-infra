# ShowMeTheMoney API 명세

Base URL: `http://localhost:8080/api`

---

## 공통

### 인증
인증이 필요한 요청은 `Authorization` 헤더에 Bearer 토큰을 포함합니다.

```
Authorization: Bearer {accessToken}
```

### 응답 형식
모든 응답은 아래 형식을 따릅니다.

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

**실패 시**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 메시지"
  }
}
```

### HTTP 상태 코드
| 코드 | 의미 |
|---|---|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 요청 유효성 오류 |
| 401 | 인증 필요 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복) |

---

## Auth

### 회원가입
인증 불필요

```
POST /auth/signup
```

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| username | String | ✅ | 사용자명 |
| name | String | ✅ | 닉네임 |
| email | String | ✅ | 이메일 |
| password | String | ✅ | 비밀번호 (8자 이상) |

```json
{
  "username": "hong123",
  "name": "홍길동",
  "email": "hong@example.com",
  "password": "password123"
}
```

**Response** `201`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 로그인
인증 불필요

```
POST /auth/login
```

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| username | String | ✅ | 사용자명 |
| password | String | ✅ | 비밀번호 |

```json
{
  "username": "hong123",
  "password": "password123"
}
```

**Response** `200`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400
  },
  "error": null
}
```

---

### 로그아웃
인증 필요

```
POST /auth/logout
```

AccessToken only 방식으로 클라이언트에서 토큰을 삭제합니다.

**Response** `200`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## Categories
인증 필요

### 카테고리 목록 조회

```
GET /categories
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| type | Integer | ❌ | `0` 지출 / `1` 수입 (생략 시 전체) |

**Response** `200`
```json
{
  "success": true,
  "data": [
    { "code": "FOOD", "codeNumber": "001", "name": "식비", "type": 0 },
    { "code": "CAFE", "codeNumber": "002", "name": "카페", "type": 0 },
    { "code": "SALARY", "codeNumber": "101", "name": "급여", "type": 1 }
  ],
  "error": null
}
```

**카테고리 시드 데이터**

지출 (`type: 0`)

| code | name |
|---|---|
| FOOD | 식비 |
| CAFE | 카페 |
| TRANSPORT | 교통 |
| SHOPPING | 쇼핑 |
| HOUSING | 주거 및 통신 |
| MEDICAL | 의료건강 |
| CULTURE | 문화생활 |
| EDUCATION | 교육 |
| ETC | 기타 |

수입 (`type: 1`)

| code | name |
|---|---|
| SALARY | 급여 |
| INVESTMENT | 투자수익 |
| ETC | 기타 |
| PART_TIME | 아르바이트 |

---

## Transactions
인증 필요

### 내역 생성

```
POST /transactions
```

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| type | Integer | ✅ | `0` 지출 / `1` 수입 |
| categoryCode | String | ✅ | 카테고리 코드 (type과 일치해야 함) |
| amount | Number | ✅ | 금액 (양수) |
| memo | String | ❌ | 메모 |
| transactionAt | String | ✅ | 날짜 (`yyyy-MM-dd`) |

```json
{
  "type": 0,
  "categoryCode": "FOOD",
  "amount": 15000,
  "memo": "점심",
  "transactionAt": "2026-06-29"
}
```

**Response** `201`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 내역 목록 조회

```
GET /transactions
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| type | Integer | ❌ | `0` 지출 / `1` 수입 |
| categoryCode | String | ❌ | 카테고리 코드 |
| period | String | ❌ | 기간 (`2026` / `2026-06` / `2026-06-29`) |
| sort | String | ❌ | `desc`(기본) / `asc` |
| page | Integer | ❌ | 페이지 번호 (기본: 0) |
| size | Integer | ❌ | 페이지 크기 (기본: 20) |

**Response** `200`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "type": "EXPENSE",
        "categoryCode": "FOOD",
        "categoryName": "식비",
        "amount": 15000,
        "memo": "점심",
        "transactionAt": "2026-06-29"
      }
    ],
    "totalElements": 72,
    "totalPages": 4,
    "totalIncome": 12890000,
    "totalExpense": 5183900
  },
  "error": null
}
```

`type`은 `"EXPENSE"`(지출) / `"INCOME"`(수입) 문자열입니다.

---

### 내역 단건 조회

```
GET /transactions/{id}
```

**Response** `200`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "type": "EXPENSE",
    "categoryCode": "FOOD",
    "categoryName": "식비",
    "amount": 15000,
    "memo": "점심",
    "transactionAt": "2026-06-29"
  },
  "error": null
}
```

---

### 내역 수정

```
PUT /transactions/{id}
```

수정할 필드만 포함합니다.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| type | Integer | ❌ | `0` 지출 / `1` 수입 |
| categoryCode | String | ❌ | 카테고리 코드 |
| amount | Number | ❌ | 금액 (양수) |
| memo | String | ❌ | 메모 |
| transactionAt | String | ❌ | 날짜 (`yyyy-MM-dd`) |

```json
{
  "amount": 20000,
  "memo": "저녁"
}
```

**Response** `200`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 내역 삭제

```
DELETE /transactions/{id}
```

소프트 삭제입니다.

**Response** `200`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## Recurring Items
인증 필요

### 고정 항목 생성

```
POST /recurring-items
```

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| type | Integer | ✅ | `0` 지출 / `1` 수입 |
| categoryCode | String | ✅ | 카테고리 코드 |
| name | String | ✅ | 항목명 |
| amount | Number | ✅ | 금액 (양수) |
| billingDay | Integer | ✅ | 결제일 (1~31) |

```json
{
  "type": 0,
  "categoryCode": "HOUSING",
  "name": "월세",
  "amount": 500000,
  "billingDay": 1
}
```

**Response** `201`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 고정 항목 목록 조회

```
GET /recurring-items
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| isActive | Boolean | ❌ | `true` 활성 / `false` 비활성 |
| type | Integer | ❌ | `0` 지출 / `1` 수입 |

**Response** `200`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "EXPENSE",
      "categoryCode": "HOUSING",
      "categoryName": "주거 및 통신",
      "name": "월세",
      "amount": 500000,
      "billingDay": 1,
      "isActive": 1
    }
  ],
  "error": null
}
```

`type`은 `"EXPENSE"`(지출) / `"INCOME"`(수입) 문자열, `isActive`는 `1`(활성) / `0`(비활성) 입니다.

---

### 고정 항목 수정

```
PUT /recurring-items/{id}
```

수정할 필드만 포함합니다.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| name | String | ❌ | 항목명 |
| amount | Number | ❌ | 금액 (양수) |
| billingDay | Integer | ❌ | 결제일 (1~31) |
| categoryCode | String | ❌ | 카테고리 코드 |
| isActive | Boolean | ❌ | 활성 여부 |

**Response** `200`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 고정 항목 삭제

```
DELETE /recurring-items/{id}
```

소프트 삭제입니다.

**Response** `200`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## Budgets
인증 필요

### 예산 설정

```
POST /budgets
```

월별 예산은 1건만 허용됩니다. 이미 존재하면 `409`를 반환합니다.

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| yearMonth | String | ✅ | 연월 (`202606` 형식) |
| amount | Number | ✅ | 예산 금액 (양수) |
| memo | String | ❌ | 메모 |

```json
{
  "yearMonth": "202606",
  "amount": 2000000
}
```

**Response** `201`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

### 예산 조회

```
GET /budgets?yearMonth={yearMonth}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| yearMonth | String | ✅ | 연월 (`202606` 형식) |

**Response** `200`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "yearMonth": "2026-06",
    "amount": 2000000
  },
  "error": null
}
```

---

### 예산 수정

```
PUT /budgets/{id}
```

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| amount | Number | ❌ | 예산 금액 (양수) |
| memo | String | ❌ | 메모 |

**Response** `200`
```json
{
  "success": true,
  "data": null,
  "error": null
}
```

---

## Dashboard
인증 필요

### 월별 요약 조회

```
GET /dashboard/summary?yearMonth={yearMonth}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| yearMonth | String | ✅ | 연월 (`202606` 형식) |

**Response** `200`
```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-06",
    "totalIncome": 3000000,
    "totalExpense": 1500000,
    "balance": 1500000,
    "budgetAmount": 2000000,
    "budgetUsageRate": 75.0,
    "isOverBudget": false
  },
  "error": null
}
```

---

### 카테고리별 지출 조회

```
GET /dashboard/categories?yearMonth={yearMonth}
```

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| yearMonth | String | ✅ | 연월 (`202606` 형식) |
| type | Integer | ❌ | `0` 지출 / `1` 수입 |

**Response** `200`
```json
{
  "success": true,
  "data": [
    {
      "categoryCode": "FOOD",
      "categoryName": "식비",
      "amount": 300000,
      "ratio": 20.0
    },
    {
      "categoryCode": "TRANSPORT",
      "categoryName": "교통",
      "amount": 150000,
      "ratio": 10.0
    }
  ],
  "error": null
}
```

---

### 일별 잔액 추이 조회

```
GET /dashboard/daily?yearMonth={yearMonth}
```

해당 월의 1일부터 말일까지 하루 단위로 수입/지출과 누적 지출·누적 잔액을 반환한다. 거래가 없는 날짜도 0으로 채워서 반환하므로 프론트엔드는 별도 보간 없이 그대로 그래프에 사용할 수 있다.

**Query Parameters**
| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| yearMonth | String | ✅ | 연월 (`202606` 형식) |

**Response** `200`
```json
{
  "success": true,
  "data": {
    "yearMonth": "2026-06",
    "budgetAmount": 2000000,
    "days": [
      {
        "date": "2026-06-01",
        "income": 0,
        "expense": 100000,
        "cumulativeExpense": 100000,
        "cumulativeBalance": -100000
      },
      {
        "date": "2026-06-02",
        "income": 3000000,
        "expense": 0,
        "cumulativeExpense": 100000,
        "cumulativeBalance": 2900000
      }
    ]
  },
  "error": null
}
```

- `budgetAmount`는 해당 월에 설정된 예산이 없으면 `null`이다.
- `cumulativeExpense`는 1일부터 해당 날짜까지의 누적 지출, `cumulativeBalance`는 누적 수입에서 누적 지출을 뺀 값이다.

---

## System

### 헬스 체크
인증 불필요

```
GET /health
```

**Response** `200`
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "db": "connected"
  },
  "error": null
}
```

DB 연결이 끊긴 경우에도 HTTP status는 `200`이며 `data.status`가 `"DOWN"`으로 내려온다. 이 엔드포인트는 HTTP 상태코드로 실패를 표현하지 않으므로, 헬스체크를 붙일 때는 `data.status` 필드를 봐야 한다.

---

## 에러 코드 카탈로그

`error.code` 값으로 나올 수 있는 전체 목록이다. 2026-07-22 기준 team4 배포서버(`http://team4.mang.pe.kr/api`)에 실제로 요청을 보내 검증했다.

| code | HTTP status | 메시지 | 실제 발생 여부 |
|---|---|---|---|
| `INVALID_INPUT` | 400 | 입력값이 올바르지 않습니다. (Bean Validation 실패 시 필드별 메시지로 대체됨) | ✅ |
| `UNAUTHORIZED` | 401 | 인증이 필요합니다. | ❌ 정의만 되어 있고 코드상 던져지는 곳이 없음. 실제 미인증 401은 아래 "Known Limitations" 참고 |
| `FORBIDDEN` | 403 | 접근 권한이 없습니다. | ✅ 다른 유저 소유 리소스 접근 시 |
| `NOT_FOUND` | 404 | 리소스를 찾을 수 없습니다. | ❌ 정의만 되어 있음. 실제로는 리소스별 `*_NOT_FOUND` 코드가 쓰임 |
| `INTERNAL_SERVER_ERROR` | 500 | 서버 오류가 발생했습니다. | ❌ 정의만 되어 있고 코드상 던져지는 곳이 없음. 처리 안 되는 예외는 Spring 기본 에러 응답으로 나감 |
| `USERNAME_ALREADY_EXISTS` | 409 | 이미 사용 중인 아이디입니다. | ✅ |
| `EMAIL_ALREADY_EXISTS` | 409 | 이미 사용 중인 이메일입니다. | ✅ |
| `INVALID_CREDENTIALS` | 401 | 아이디 또는 비밀번호가 올바르지 않습니다. | ✅ (아이디 미존재/비밀번호 오류 모두 동일 코드) |
| `INVALID_TOKEN` | 401 | 유효하지 않은 토큰입니다. | ❌ 정의만 되어 있음. 실제 잘못된 토큰은 아래 "Known Limitations" 참고 |
| `EXPIRED_TOKEN` | 401 | 만료된 토큰입니다. | ❌ 정의만 되어 있음. 만료 토큰도 무효 토큰과 동일하게 처리됨 |
| `USER_NOT_FOUND` | 404 | 사용자를 찾을 수 없습니다. | (`/users/me` 전용 — 이번 문서화 범위 밖) |
| `CATEGORY_NOT_FOUND` | 404 | 카테고리를 찾을 수 없습니다. | ✅ |
| `CATEGORY_TYPE_MISMATCH` | 400 | 거래 유형과 카테고리 유형이 일치하지 않습니다. | ❌ 정의만 되어 있음. 실제로 카테고리-유형 불일치 시에도 `CATEGORY_NOT_FOUND`가 나감 |
| `TRANSACTION_NOT_FOUND` | 404 | 거래 내역을 찾을 수 없습니다. | ✅ |
| `INVALID_PERIOD_FORMAT` | 400 | period 형식이 올바르지 않습니다. | ❌ 정의만 되어 있고 코드상 던져지는 곳이 없음 |
| `RECURRING_ITEM_NOT_FOUND` | 404 | 고정 항목을 찾을 수 없습니다. | ✅ |
| `BUDGET_NOT_FOUND` | 404 | 예산을 찾을 수 없습니다. | ✅ |
| `BUDGET_ALREADY_EXISTS` | 409 | 해당 월의 예산이 이미 존재합니다. | ✅ |

---

## 실패 케이스 검증 결과

2026-07-22, team4 배포서버 기준으로 아래 실패 시나리오를 실제 실행해 응답을 확인했다. (`INVALID_INPUT`류는 Bean Validation 필드 메시지가 그대로 노출되므로 필드마다 문구가 다를 수 있다.)

**Auth**
| 케이스 | 요청 | status | error.code |
|---|---|---|---|
| 필수 필드 누락 | `POST /auth/signup {}` | 400 | `INVALID_INPUT` |
| 이메일 형식 오류 | `POST /auth/signup` (email: `not-an-email`) | 400 | `INVALID_INPUT` |
| 비밀번호 8자 미만 | `POST /auth/signup` (password: `123`) | 400 | `INVALID_INPUT` |
| username 중복 | `POST /auth/signup` | 409 | `USERNAME_ALREADY_EXISTS` |
| email 중복 | `POST /auth/signup` | 409 | `EMAIL_ALREADY_EXISTS` |
| 비밀번호 오류 | `POST /auth/login` | 401 | `INVALID_CREDENTIALS` |
| 존재하지 않는 username | `POST /auth/login` | 401 | `INVALID_CREDENTIALS` |

**Transactions**
| 케이스 | 요청 | status | error.code |
|---|---|---|---|
| 필수 필드 누락 | `POST /transactions {}` | 400 | `INVALID_INPUT` |
| amount 음수 | `POST /transactions` (amount: -100) | 400 | `INVALID_INPUT` |
| 존재하지 않는 categoryCode | `POST /transactions` | 404 | `CATEGORY_NOT_FOUND` |
| categoryCode-type 불일치 | `POST /transactions` (type=1, categoryCode=지출카테고리) | 404 | `CATEGORY_NOT_FOUND` (⚠ `CATEGORY_TYPE_MISMATCH` 아님) |
| 존재하지 않는 id | `GET/PUT /transactions/{id}` | 404 | `TRANSACTION_NOT_FOUND` |
| 다른 유저 소유 id | `GET/DELETE /transactions/{id}` | 403 | `FORBIDDEN` |

**Recurring Items**
| 케이스 | 요청 | status | error.code |
|---|---|---|---|
| billingDay 범위 밖(35) | `POST /recurring-items` | 400 | `INVALID_INPUT` |
| 존재하지 않는 categoryCode | `POST /recurring-items` | 404 | `CATEGORY_NOT_FOUND` |
| 존재하지 않는 id | `PUT /recurring-items/{id}` | 404 | `RECURRING_ITEM_NOT_FOUND` |
| 다른 유저 소유 id | `DELETE /recurring-items/{id}` | 403 | `FORBIDDEN` |

**Budgets**
| 케이스 | 요청 | status | error.code |
|---|---|---|---|
| yearMonth 길이 오류(5자/7자) | `POST /budgets` | 400 | `INVALID_INPUT` |
| 같은 달 중복 생성 | `POST /budgets` | 409 | `BUDGET_ALREADY_EXISTS` |
| 존재하지 않는 yearMonth | `GET /budgets?yearMonth=...` | 404 | `BUDGET_NOT_FOUND` |
| 다른 유저 소유 id | `PUT /budgets/{id}` | 403 | `FORBIDDEN` |
| 존재하지 않는 id | `PUT /budgets/{id}` | 404 | `BUDGET_NOT_FOUND` |

**Dashboard**
| 케이스 | 요청 | status | error.code |
|---|---|---|---|
| yearMonth 형식 오류 | `GET /dashboard/summary?yearMonth=abc` | 400 | `INVALID_INPUT` |
| yearMonth 형식 오류 | `GET /dashboard/categories?yearMonth=abc` | 400 | `INVALID_INPUT` |

**공통 인증 실패**
| 케이스 | 요청 | status | 응답 body |
|---|---|---|---|
| 토큰 없음 | 인증 필요 API 호출 | 401 | 빈 본문 (`content-length: 0`) — 아래 Known Limitations 참고 |
| 잘못된 토큰 | `Authorization: Bearer garbage.invalid.token` | 401 | 빈 본문 |

---

## Known Limitations

문서화 과정에서 실제 확인된, 현재 구현의 알려진 동작 차이다.

1. ~~인증된 요청도 파라미터 바인딩 오류 시 401(빈 본문)이 반환됨~~ **[Fixed]**
   `MethodArgumentTypeMismatchException`(예: `?type=abc`) · `MissingServletRequestParameterException`(필수 쿼리 파라미터 누락) 발생 시 `GlobalExceptionHandler`가 처리하지 않아 Spring Security의 `AuthenticationEntryPoint`로 잘못 위임되어 본문 없는 401이 나가던 문제. `GlobalExceptionHandler`에 두 예외 핸들러를 추가해 `400 INVALID_INPUT`(공통 응답 포맷)으로 반환하도록 수정함 (이슈 #7). 로컬에 실제 백엔드+MySQL을 띄워 수정 전(401 재현)/후(400 정상)를 curl로 직접 검증, 회귀 테스트 통과.
2. ~~`/budgets`의 `yearMonth`가 숫자 여부를 검증하지 않음~~ **[Fixed]**
   `POST /budgets {"yearMonth":"abcdef", "amount":1000}`처럼 문자 6자리도 통과해 DB에 `"abcd-ef"`로 저장되던 문제. `BudgetService`/`DashboardService`에 중복돼 있던 변환 로직을 `YearMonthKey` 공통 유틸로 추출하고 정규식(`^\d{6}$`) 검증으로 교체함 (이슈 #8).
3. **카테고리-거래유형 불일치는 `CATEGORY_TYPE_MISMATCH`가 아니라 `CATEGORY_NOT_FOUND`로 나간다.** (에러 코드 카탈로그 참고, 아직 미수정)
4. ~~`GET /health`는 DB 연결 실패 시에도 HTTP `200`을 반환한다~~ **[Fixed]** — DB 연결 실패 시 `503`을 반환하도록 수정됨 (PR #9, `fix/health-check-status`).
5. **토큰이 아예 없거나 형식이 잘못된 경우(`Authorization` 헤더 없음 / `Bearer` 아닌 값 / 파싱 불가 토큰) 401 응답의 본문이 비어있다.** 이건 위 1번과 달리 `SecurityConfig`의 `authenticationEntryPoint`(`res.sendError(401)`)가 의도한 그대로 동작하는 것이며, 아직 공통 응답 포맷을 따르지 않는 별개의 항목이다. 필요하면 `authenticationEntryPoint`에서도 `ApiResponse` 포맷으로 응답하도록 수정 검토.
