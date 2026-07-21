-- ============================================================
-- 더미 데이터 시드 (로컬 개발 / QA 용)
-- 공통 비밀번호: password123
-- ============================================================

-- ─────────────────────────────────────────
-- 1. 사용자
-- ─────────────────────────────────────────
INSERT IGNORE INTO users (username, email, password, nickname) VALUES
    ('testuser', 'test@example.com',  '$2a$10$0FCRJE2JjUVEBh1oGeSUSe2GBOa7FPUHkR1M/7M2HP.S8Bsmr1TJu', '테스터'),
    ('alice',    'alice@example.com', '$2a$10$0FCRJE2JjUVEBh1oGeSUSe2GBOa7FPUHkR1M/7M2HP.S8Bsmr1TJu', '앨리스'),
    ('bob',      'bob@example.com',   '$2a$10$0FCRJE2JjUVEBh1oGeSUSe2GBOa7FPUHkR1M/7M2HP.S8Bsmr1TJu', '밥');

-- 카테고리 UUID 변수화 (ID 순서에 의존하지 않도록)
SET @c_food      = (SELECT uuid FROM categories WHERE code = 'FOOD'       AND type = 0);
SET @c_cafe      = (SELECT uuid FROM categories WHERE code = 'CAFE'       AND type = 0);
SET @c_transport = (SELECT uuid FROM categories WHERE code = 'TRANSPORT'  AND type = 0);
SET @c_shopping  = (SELECT uuid FROM categories WHERE code = 'SHOPPING'   AND type = 0);
SET @c_housing   = (SELECT uuid FROM categories WHERE code = 'HOUSING'    AND type = 0);
SET @c_medical   = (SELECT uuid FROM categories WHERE code = 'MEDICAL'    AND type = 0);
SET @c_culture   = (SELECT uuid FROM categories WHERE code = 'CULTURE'    AND type = 0);
SET @c_education = (SELECT uuid FROM categories WHERE code = 'EDUCATION'  AND type = 0);
SET @c_etc_exp   = (SELECT uuid FROM categories WHERE code = 'ETC'        AND type = 0);
SET @c_salary    = (SELECT uuid FROM categories WHERE code = 'SALARY'     AND type = 1);
SET @c_invest    = (SELECT uuid FROM categories WHERE code = 'INVESTMENT' AND type = 1);
SET @c_etc_inc   = (SELECT uuid FROM categories WHERE code = 'ETC'        AND type = 1);
SET @c_parttime  = (SELECT uuid FROM categories WHERE code = 'PART_TIME'  AND type = 1);

-- 사용자 UUID 변수화
SET @u_test  = (SELECT uuid FROM users WHERE username = 'testuser');
SET @u_alice = (SELECT uuid FROM users WHERE username = 'alice');
SET @u_bob   = (SELECT uuid FROM users WHERE username = 'bob');


-- ─────────────────────────────────────────
-- 2. 거래 내역 — testuser (3개월)
-- ─────────────────────────────────────────

-- 2026-04 수입
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_salary,   1, 3000000, '4월 급여',        '2026-04-25'),
    (@u_test, @c_parttime, 1,  350000, '주말 알바',        '2026-04-30');

-- 2026-04 지출
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_housing,   0,  650000, '4월 월세',         '2026-04-01'),
    (@u_test, @c_culture,   0,   55000, '헬스장 월정액',    '2026-04-01'),
    (@u_test, @c_transport, 0,    8500, '교통카드 충전',     '2026-04-01'),
    (@u_test, @c_food,      0,   38000, '편의점 장보기',    '2026-04-02'),
    (@u_test, @c_cafe,      0,    8500, '아메리카노',       '2026-04-03'),
    (@u_test, @c_culture,   0,   17000, '넷플릭스',         '2026-04-05'),
    (@u_test, @c_housing,   0,   55000, '핸드폰 요금',      '2026-04-05'),
    (@u_test, @c_food,      0,   65000, '주말 외식',        '2026-04-05'),
    (@u_test, @c_transport, 0,    2700, '버스비',           '2026-04-07'),
    (@u_test, @c_food,      0,   12500, '점심',             '2026-04-08'),
    (@u_test, @c_cafe,      0,    7000, '카페라떼',         '2026-04-10'),
    (@u_test, @c_food,      0,   45000, '마트 장보기',      '2026-04-12'),
    (@u_test, @c_shopping,  0,   89000, '봄 옷',            '2026-04-14'),
    (@u_test, @c_etc_exp,   0,   25000, '잡비',             '2026-04-20');

-- 2026-05 수입
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_salary, 1, 3000000, '5월 급여',    '2026-05-25'),
    (@u_test, @c_invest, 1,  120000, '배당금 수령',  '2026-05-15');

-- 2026-05 지출
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_housing,   0,  650000, '5월 월세',         '2026-05-01'),
    (@u_test, @c_culture,   0,   55000, '헬스장 월정액',    '2026-05-01'),
    (@u_test, @c_transport, 0,    8500, '교통카드 충전',     '2026-05-01'),
    (@u_test, @c_education, 0,   80000, '온라인 강의',      '2026-05-02'),
    (@u_test, @c_food,      0,   42000, '마트',             '2026-05-03'),
    (@u_test, @c_cafe,      0,   12000, '아이스아메리카노', '2026-05-04'),
    (@u_test, @c_culture,   0,   17000, '넷플릭스',         '2026-05-05'),
    (@u_test, @c_housing,   0,   55000, '핸드폰 요금',      '2026-05-05'),
    (@u_test, @c_medical,   0,   32000, '병원비',           '2026-05-08'),
    (@u_test, @c_medical,   0,   15000, '약국',             '2026-05-09'),
    (@u_test, @c_shopping,  0,  156000, '가전제품',         '2026-05-10'),
    (@u_test, @c_food,      0,   18000, '편의점',           '2026-05-07'),
    (@u_test, @c_food,      0,   75000, '가족 외식',        '2026-05-11'),
    (@u_test, @c_transport, 0,    4500, '버스비',           '2026-05-14'),
    (@u_test, @c_food,      0,   28500, '점심',             '2026-05-16'),
    (@u_test, @c_cafe,      0,    9500, '카페',             '2026-05-18'),
    (@u_test, @c_food,      0,   35000, '마트',             '2026-05-22'),
    (@u_test, @c_shopping,  0,   45000, '생활용품',         '2026-05-24'),
    (@u_test, @c_etc_exp,   0,    8000, '잡비',             '2026-05-28');

-- 2026-06 수입
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_salary,   1, 3000000, '6월 급여',     '2026-06-25'),
    (@u_test, @c_parttime, 1,  420000, '주말 알바',     '2026-06-15');

-- 2026-06 지출
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_housing,   0,  650000, '6월 월세',         '2026-06-01'),
    (@u_test, @c_culture,   0,   55000, '헬스장 월정액',    '2026-06-01'),
    (@u_test, @c_transport, 0,    8500, '교통카드 충전',     '2026-06-01'),
    (@u_test, @c_food,      0,   55000, '마트 장보기',      '2026-06-01'),
    (@u_test, @c_cafe,      0,   11000, '커피',             '2026-06-02'),
    (@u_test, @c_education, 0,  120000, '자격증 강의',      '2026-06-03'),
    (@u_test, @c_food,      0,   22000, '편의점',           '2026-06-05'),
    (@u_test, @c_culture,   0,   17000, '넷플릭스',         '2026-06-05'),
    (@u_test, @c_housing,   0,   55000, '핸드폰 요금',      '2026-06-05'),
    (@u_test, @c_shopping,  0,  235000, '여름 옷',          '2026-06-07'),
    (@u_test, @c_food,      0,   68000, '주말 외식',        '2026-06-08'),
    (@u_test, @c_transport, 0,    3500, '버스비',           '2026-06-10'),
    (@u_test, @c_food,      0,   31000, '점심',             '2026-06-12'),
    (@u_test, @c_culture,   0,   42000, '영화 및 식사',     '2026-06-14'),
    (@u_test, @c_cafe,      0,    8500, '카페',             '2026-06-16'),
    (@u_test, @c_food,      0,   48000, '마트',             '2026-06-18'),
    (@u_test, @c_transport, 0,    6200, '택시',             '2026-06-20'),
    (@u_test, @c_shopping,  0,   88000, '생활용품',         '2026-06-21'),
    (@u_test, @c_etc_exp,   0,   15000, '잡비',             '2026-06-25');


-- ─────────────────────────────────────────
-- 3. 거래 내역 — alice (2개월)
-- ─────────────────────────────────────────

-- 2026-05
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_alice, @c_salary,   1, 2500000, '5월 급여',    '2026-05-25'),
    (@u_alice, @c_housing,  0,  500000, '월세',         '2026-05-01'),
    (@u_alice, @c_food,     0,   35000, '마트',         '2026-05-03'),
    (@u_alice, @c_transport,0,    9000, '교통비',       '2026-05-05'),
    (@u_alice, @c_food,     0,   28000, '편의점',       '2026-05-15'),
    (@u_alice, @c_shopping, 0,   95000, '쇼핑',         '2026-05-20');

-- 2026-06
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_alice, @c_salary,   1, 2500000, '6월 급여',     '2026-06-25'),
    (@u_alice, @c_housing,  0,  500000, '월세',          '2026-06-01'),
    (@u_alice, @c_food,     0,   42000, '마트',          '2026-06-04'),
    (@u_alice, @c_transport,0,    8000, '교통비',        '2026-06-03'),
    (@u_alice, @c_culture,  0,   17000, '넷플릭스',      '2026-06-05'),
    (@u_alice, @c_cafe,     0,   15000, '카페',          '2026-06-10'),
    (@u_alice, @c_shopping, 0,  125000, '쇼핑',          '2026-06-12'),
    (@u_alice, @c_food,     0,   38500, '외식',          '2026-06-18');


-- ─────────────────────────────────────────
-- 4. 거래 내역 — bob (1개월)
-- ─────────────────────────────────────────

INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_bob, @c_salary,   1, 3500000, '6월 급여',   '2026-06-25'),
    (@u_bob, @c_etc_inc,  1,  200000, '용돈',        '2026-06-01'),
    (@u_bob, @c_housing,  0,  700000, '월세',        '2026-06-01'),
    (@u_bob, @c_food,     0,   52000, '마트',        '2026-06-02'),
    (@u_bob, @c_food,     0,   78000, '외식',        '2026-06-08'),
    (@u_bob, @c_shopping, 0,  320000, '가전제품',    '2026-06-10'),
    (@u_bob, @c_transport,0,   12000, '교통비',      '2026-06-12'),
    (@u_bob, @c_medical,  0,   45000, '병원비',      '2026-06-20');


-- ─────────────────────────────────────────
-- 5. 고정 수입/지출 — testuser
-- ─────────────────────────────────────────

INSERT IGNORE INTO recurring_items (uuid_user, uuid_category, type, name, amount, billing_day, is_active) VALUES
    (@u_test, @c_housing,   0, '월세',              650000,  1, 1),
    (@u_test, @c_culture,   0, '헬스장',             55000,  1, 1),
    (@u_test, @c_culture,   0, '넷플릭스',           17000,  5, 1),
    (@u_test, @c_housing,   0, '핸드폰 요금',        55000,  5, 1),
    (@u_test, @c_salary,    1, '급여',             3000000, 25, 1),
    (@u_test, @c_transport, 0, '교통카드 자동충전',   50000, 10, 0),
    (@u_test, @c_education, 0, '온라인 강의 구독',    80000,  2, 0);


-- ─────────────────────────────────────────
-- 6. 고정 수입/지출 — alice
-- ─────────────────────────────────────────

INSERT IGNORE INTO recurring_items (uuid_user, uuid_category, type, name, amount, billing_day, is_active) VALUES
    (@u_alice, @c_housing, 0, '월세',    500000,  1, 1),
    (@u_alice, @c_culture, 0, '넷플릭스', 17000,  5, 1),
    (@u_alice, @c_salary,  1, '급여',   2500000, 25, 1);


-- ─────────────────────────────────────────
-- 7. 거래 내역 — testuser 2026-03 (예산 초과 시나리오)
-- ─────────────────────────────────────────

-- 수입
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_salary, 1, 3000000, '3월 급여', '2026-03-25');

-- 지출 (합계 약 1,220,000 — 예산 1,000,000 초과)
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_housing,   0, 650000, '3월 월세',      '2026-03-01'),
    (@u_test, @c_culture,   0,  55000, '헬스장 월정액', '2026-03-01'),
    (@u_test, @c_transport, 0,   8500, '교통카드 충전', '2026-03-01'),
    (@u_test, @c_food,      0,  45000, '마트 장보기',   '2026-03-03'),
    (@u_test, @c_culture,   0,  17000, '넷플릭스',      '2026-03-05'),
    (@u_test, @c_housing,   0,  55000, '핸드폰 요금',   '2026-03-05'),
    (@u_test, @c_food,      0,  65000, '주말 외식',     '2026-03-08'),
    (@u_test, @c_cafe,      0,  12000, '카페',          '2026-03-10'),
    (@u_test, @c_shopping,  0, 180000, '의류 쇼핑',     '2026-03-15'),
    (@u_test, @c_food,      0,  38000, '편의점',        '2026-03-18'),
    (@u_test, @c_cafe,      0,   8500, '아메리카노',    '2026-03-20'),
    (@u_test, @c_food,      0,  52000, '가족 외식',     '2026-03-22'),
    (@u_test, @c_culture,   0,  35000, '영화 관람',     '2026-03-28');


-- ─────────────────────────────────────────
-- 8. 예산 — testuser (4개월, 2026-03 초과 포함)
-- ─────────────────────────────────────────

INSERT IGNORE INTO budgets (uuid_user, `year_month`, amount) VALUES
    (@u_test, '2026-03', 1000000),
    (@u_test, '2026-04', 2000000),
    (@u_test, '2026-05', 2000000),
    (@u_test, '2026-06', 2500000);


-- ─────────────────────────────────────────
-- 9. 예산 — alice (2개월)
-- ─────────────────────────────────────────

INSERT IGNORE INTO budgets (uuid_user, `year_month`, amount) VALUES
    (@u_alice, '2026-05', 1500000),
    (@u_alice, '2026-06', 1500000);
