-- 지출 카테고리 (type = 0)
INSERT INTO categories (code, code_number, name, type) VALUES
    ('FOOD',      '001', '식비',        0),
    ('CAFE',      '002', '카페',        0),
    ('TRANSPORT', '003', '교통',        0),
    ('SHOPPING',  '004', '쇼핑',        0),
    ('HOUSING',   '005', '주거 및 통신', 0),
    ('MEDICAL',   '006', '의료건강',    0),
    ('CULTURE',   '007', '문화생활',    0),
    ('EDUCATION', '008', '교육',        0),
    ('ETC',       '009', '기타',        0);

-- 수입 카테고리 (type = 1)
INSERT INTO categories (code, code_number, name, type) VALUES
    ('SALARY',     '101', '급여',      1),
    ('INVESTMENT', '102', '투자수익',  1),
    ('ETC',        '103', '기타',      1),
    ('PART_TIME',  '104', '아르바이트', 1);
