-- ============================================================
-- 7월 더미 데이터 추가 (일별 잔액 차트 확인용)
-- ============================================================

SET @c_food      = (SELECT uuid FROM categories WHERE code = 'FOOD'       AND type = 0);
SET @c_cafe      = (SELECT uuid FROM categories WHERE code = 'CAFE'       AND type = 0);
SET @c_transport = (SELECT uuid FROM categories WHERE code = 'TRANSPORT'  AND type = 0);
SET @c_shopping  = (SELECT uuid FROM categories WHERE code = 'SHOPPING'   AND type = 0);
SET @c_medical   = (SELECT uuid FROM categories WHERE code = 'MEDICAL'    AND type = 0);
SET @c_culture   = (SELECT uuid FROM categories WHERE code = 'CULTURE'    AND type = 0);
SET @c_education = (SELECT uuid FROM categories WHERE code = 'EDUCATION'  AND type = 0);
SET @c_etc_exp   = (SELECT uuid FROM categories WHERE code = 'ETC'        AND type = 0);
SET @c_parttime  = (SELECT uuid FROM categories WHERE code = 'PART_TIME'  AND type = 1);

SET @u_test  = (SELECT uuid FROM users WHERE username = 'testuser');

-- 월세/헬스장(billing_day 1), 넷플릭스/핸드폰 요금(billing_day 5), 급여(billing_day 25)는
-- testuser의 활성 고정 수입/지출 항목이라 RecurringItemBillingScheduler가 해당 결제일에
-- 실제 거래를 자동 생성한다. 여기서 같은 항목을 또 심으면 대시보드 합계가 중복 집계되므로
-- 이 마이그레이션에는 넣지 않는다 (스케줄러가 아직 지나지 않은 날짜는 자연히 비어 있는다).

-- ─────────────────────────────────────────
-- 2026-07 수입 — testuser
-- ─────────────────────────────────────────
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_parttime, 1,  300000, '주말 알바', '2026-07-19');

-- ─────────────────────────────────────────
-- 2026-07 지출 — testuser (1일~24일, 고정 지출 항목 제외)
-- ─────────────────────────────────────────
INSERT IGNORE INTO transactions (uuid_user, uuid_category, type, amount, memo, transaction_date) VALUES
    (@u_test, @c_transport, 0,    8500, '교통카드 충전',     '2026-07-01'),
    (@u_test, @c_food,      0,   42000, '마트 장보기',      '2026-07-02'),
    (@u_test, @c_cafe,      0,    9500, '아메리카노',       '2026-07-03'),
    (@u_test, @c_food,      0,   68000, '주말 외식',        '2026-07-06'),
    (@u_test, @c_transport, 0,    3200, '버스비',           '2026-07-08'),
    (@u_test, @c_food,      0,   13500, '점심',             '2026-07-09'),
    (@u_test, @c_shopping,  0,  120000, '여름 옷',          '2026-07-10'),
    (@u_test, @c_cafe,      0,    8000, '카페라떼',         '2026-07-11'),
    (@u_test, @c_food,      0,   48000, '마트 장보기',      '2026-07-12'),
    (@u_test, @c_medical,   0,   22000, '병원비',           '2026-07-13'),
    (@u_test, @c_education, 0,   90000, '온라인 강의',      '2026-07-14'),
    (@u_test, @c_food,      0,   26000, '편의점',           '2026-07-15'),
    (@u_test, @c_culture,   0,   45000, '영화 및 식사',     '2026-07-16'),
    (@u_test, @c_transport, 0,    5400, '택시',             '2026-07-17'),
    (@u_test, @c_food,      0,   33000, '점심',             '2026-07-18'),
    (@u_test, @c_shopping,  0,   67000, '생활용품',         '2026-07-20'),
    (@u_test, @c_cafe,      0,   11000, '커피',             '2026-07-21'),
    (@u_test, @c_food,      0,   52000, '주말 장보기',      '2026-07-22'),
    (@u_test, @c_etc_exp,   0,   18000, '잡비',             '2026-07-23'),
    (@u_test, @c_food,      0,   29000, '점심',             '2026-07-24');

-- ─────────────────────────────────────────
-- 예산 — testuser 2026-07
-- ─────────────────────────────────────────
INSERT IGNORE INTO budgets (uuid_user, `year_month`, amount) VALUES
    (@u_test, '2026-07', 2500000);
