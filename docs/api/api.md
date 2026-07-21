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
