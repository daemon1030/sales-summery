-- local 프로필에서만 사용하는 샘플 계정과 기본 항목이다.
INSERT INTO users (login_id, password_hash, name, status, settlement_start_day, created_at, updated_at)
SELECT 'sample_1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
       '샘플 가게', 'ACTIVE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM users WHERE login_id = 'sample_1');

INSERT INTO financial_categories
    (user_id, category_name, transaction_type, cost_type, frequency, is_active, created_at, updated_at)
SELECT u.user_id, v.category_name, v.transaction_type, v.cost_type, v.frequency,
       TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
CROSS JOIN (VALUES
    ('카드 매출', 'INCOME', 'NONE', 'DAILY'),
    ('현금 매출', 'INCOME', 'NONE', 'DAILY'),
    ('재료비', 'EXPENSE', 'VARIABLE', 'IRREGULAR'),
    ('인건비', 'EXPENSE', 'FIXED', 'MONTHLY'),
    ('월세', 'EXPENSE', 'FIXED', 'MONTHLY')
) v(category_name, transaction_type, cost_type, frequency)
WHERE u.login_id = 'sample_1'
  AND NOT EXISTS (
      SELECT 1 FROM financial_categories c
      WHERE c.user_id = u.user_id AND c.category_name = v.category_name
  );
