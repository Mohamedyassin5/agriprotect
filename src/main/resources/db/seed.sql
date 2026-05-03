-- =============================================================
-- AgriProtect - Seed Data pour tests rapides
-- Mot de passe : password123
-- Login : farmer@test.com / password123
-- =============================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE budget;
TRUNCATE TABLE accounting_entry;
TRUNCATE TABLE savings_goals;
TRUNCATE TABLE savings_transaction;
TRUNCATE TABLE savings_account;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================
-- 1. USER
-- =============================================================
INSERT INTO users (id, email, password, first_name, last_name, role, score, phone_number, address, status, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440000',
  'farmer@test.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'Ahmed',
  'Ben Ali',
  'FARMER',
  72.5,
  '12345678',
  'Tunis, Tunisia',
  'ACTIVE',
  NOW(),
  NOW()
);

-- =============================================================
-- 2. SAVINGS ACCOUNT
-- =============================================================
INSERT INTO savings_account (id, user_id, account_name, current_balance, monthly_savings_target, goal_amount, goal_title, status, created_at, updated_at)
VALUES (
  1,
  '550e8400-e29b-41d4-a716-446655440000',
  'Epargne exploitation agricole',
  3800.00,
  500.00,
  18000.00,
  '3 objectif(s): Achat tracteur, Fonds urgence, Systeme irrigation',
  'ACTIVE',
  NOW(),
  NOW()
);

-- =============================================================
-- 4. SAVINGS GOALS
-- =============================================================
INSERT INTO savings_goals (id, savings_account_id, goal_name, target_amount, current_amount, target_date, description, achieved, collected, priority, created_at, updated_at)
VALUES
  ('goal-0001-0000-0000-000000000001', 1, 'Achat tracteur',       10000.00, 1900.00, '2026-12-31', 'Remplacement tracteur ancien',       false, false, 1, NOW(), NOW()),
  ('goal-0001-0000-0000-000000000002', 1, 'Fonds urgence',         5000.00,  950.00, '2026-09-30', 'Reserve pour imprevus exploitation', false, false, 2, NOW(), NOW()),
  ('goal-0001-0000-0000-000000000003', 1, 'Systeme irrigation',   3000.00,  950.00, '2026-06-30', 'Irrigation goutte a goutte',         false, false, 3, NOW(), NOW());

-- =============================================================
-- 5. SAVINGS TRANSACTIONS
-- =============================================================
INSERT INTO savings_transaction (account_id, type, amount, description, occurred_at, created_at)
VALUES
  (1, 'DEPOSIT',    1500.00, 'Epargne janvier',         '2026-01-05 10:00:00', NOW()),
  (1, 'DEPOSIT',    1200.00, 'Epargne fevrier',         '2026-02-03 10:00:00', NOW()),
  (1, 'DEPOSIT',    1500.00, 'Epargne mars',            '2026-03-01 10:00:00', NOW()),
  (1, 'WITHDRAWAL',  400.00, 'Achat semences urgence',  '2026-02-20 14:00:00', NOW());

-- =============================================================
-- 6. ACCOUNTING ENTRIES
-- =============================================================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at)
VALUES
  -- INCOME
  ('550e8400-e29b-41d4-a716-446655440000', 'INCOME',  'SALES',      4500.00, 'Vente tomates marche local',    '2026-01-10', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'INCOME',  'SALES',      3200.00, 'Vente pommes de terre',         '2026-01-25', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'INCOME',  'SALES',      5100.00, 'Vente produits fevrier',        '2026-02-12', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'INCOME',  'SALES',      2800.00, 'Vente legumes mars',            '2026-03-05', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'INCOME',  'OTHER',       600.00, 'Subvention agricole',           '2026-02-01', 'MANUAL', NOW()),
  -- EXPENSE
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'SEEDS',       350.00, 'Achat semences tomates',        '2026-01-08', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'FERTILIZER',  280.00, 'Engrais NPK',                   '2026-01-15', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'LABOR',       500.00, 'Salaires ouvriers janvier',     '2026-01-31', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'IRRIGATION',  150.00, 'Facture eau janvier',           '2026-01-20', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'SEEDS',       420.00, 'Semences fevrier',              '2026-02-05', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'LABOR',       500.00, 'Salaires ouvriers fevrier',     '2026-02-28', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'TRANSPORT',   200.00, 'Transport marche fevrier',      '2026-02-18', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'EQUIPMENT',   750.00, 'Reparation motopompe',          '2026-03-10', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'FERTILIZER',  310.00, 'Engrais mars',                  '2026-03-12', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'LABOR',       500.00, 'Salaires ouvriers mars',        '2026-03-25', 'MANUAL', NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'EXPENSE', 'INSURANCE',   400.00, 'Assurance recolte annuelle',    '2026-01-02', 'MANUAL', NOW());

-- =============================================================
-- 7. BUDGETS
-- =============================================================
INSERT INTO budget (user_id, period_type, period_start, period_end, category, planned_amount, created_at, updated_at)
VALUES
  ('550e8400-e29b-41d4-a716-446655440000', 'MONTHLY', '2026-03-01', '2026-03-31', 'SEEDS',       400.00, NOW(), NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'MONTHLY', '2026-03-01', '2026-03-31', 'FERTILIZER',  350.00, NOW(), NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'MONTHLY', '2026-03-01', '2026-03-31', 'LABOR',       600.00, NOW(), NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'MONTHLY', '2026-03-01', '2026-03-31', 'EQUIPMENT',   500.00, NOW(), NOW()),
  ('550e8400-e29b-41d4-a716-446655440000', 'MONTHLY', '2026-03-01', '2026-03-31', 'TRANSPORT',   200.00, NOW(), NOW());
