-- ============================================================
-- AgriProtect — Seed complet pret a l'emploi
-- Login : farmer@test.com / password123
-- Periode : Septembre 2025 → Mars 2026
-- ============================================================
-- INSTRUCTIONS :
-- 1. Ouvrir MySQL Workbench ou phpMyAdmin
-- 2. Selectionner la base : USE agriprotect;
-- 3. Executer tout le script
-- ============================================================

USE agriprotect;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- NETTOYAGE
-- ============================================================
DELETE FROM savings_goals;
DELETE FROM savings_transaction;
DELETE FROM savings_account;
DELETE FROM budget;
DELETE FROM accounting_entry;
DELETE FROM users WHERE email = 'farmer@test.com';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- PARTIE 0 : USER
-- password = "password123" (BCrypt)
-- ============================================================
INSERT INTO users (id, email, password, first_name, last_name, role, score, phone_number, address, status, created_at, updated_at)
VALUES (
  '550e8400-e29b-41d4-a716-446655440000',
  'farmer@test.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
  'Ahmed', 'Ben Ali', 'FARMER', 72.5, '12345678', 'Tunis, Tunisia',
  'ACTIVE', NOW(), NOW()
);

SET @USER_ID = '550e8400-e29b-41d4-a716-446655440000';

-- ============================================================
-- PARTIE 1 : SAVINGS ACCOUNT
-- ============================================================
INSERT INTO savings_account (user_id, account_name, current_balance, monthly_savings_target, goal_amount, goal_title, status, created_at, updated_at)
VALUES (@USER_ID, 'Epargne exploitation agricole', 0.00, 500.00, NULL, NULL, 'ACTIVE', NOW(), NOW());

SET @ACCOUNT_ID = LAST_INSERT_ID();

-- ============================================================
-- PARTIE 2 : ACCOUNTING ENTRIES (6 mois)
-- ============================================================

-- ==================== SEPTEMBRE 2025 ====================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      2200.00, 'Vente reste cereales ete 2025',           '2025-09-05', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       800.00, 'Vente legumes potager',                   '2025-09-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       650.00, 'Achat semences ble dur saison 2025-2026', '2025-09-03', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       280.00, 'Semences orge',                           '2025-09-04', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  420.00, 'Engrais NPK preparation sol',             '2025-09-08', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   350.00, 'Reparation tracteur',                     '2025-09-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       400.00, 'Main d oeuvre labourage',                 '2025-09-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  180.00, 'Facture eau irrigation septembre',        '2025-09-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   120.00, 'Gasoil tracteur et transport',            '2025-09-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'INSURANCE',   200.00, 'Assurance recolte trimestrielle',         '2025-09-25', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        85.00, 'Frais divers septembre',                  '2025-09-28', 'MANUAL', NOW());

-- ==================== OCTOBRE 2025 ====================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      1500.00, 'Vente legumes marche local',              '2025-10-08', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       600.00, 'Vente fourrage',                          '2025-10-22', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       180.00, 'Semences complementaires',                '2025-10-02', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  350.00, 'Amendement calcaire et fumier',           '2025-10-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  220.00, 'Facture eau irrigation octobre',          '2025-10-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       500.00, 'Journaliers pour semis',                  '2025-10-14', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   150.00, 'Transport semences et materiel',          '2025-10-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   200.00, 'Achat pieces systeme irrigation',         '2025-10-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LOAN_PAYMENT',450.00, 'Echeance credit agricole octobre',        '2025-10-25', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        70.00, 'Frais divers octobre',                    '2025-10-28', 'MANUAL', NOW());

-- ==================== NOVEMBRE 2025 ====================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      3500.00, 'Premiere vente huile olive',              '2025-11-10', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',      1200.00, 'Vente olives de table',                   '2025-11-20', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       400.00, 'Vente legumes',                           '2025-11-25', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       800.00, 'Equipe cueillette olives 10 jours',       '2025-11-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       600.00, 'Journaliers cueillette suite',            '2025-11-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   300.00, 'Transport olives vers huilerie',          '2025-11-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   250.00, 'Location materiel cueillette',            '2025-11-04', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  200.00, 'Engrais entretien oliviers',              '2025-11-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  150.00, 'Facture eau novembre',                    '2025-11-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'INSURANCE',   200.00, 'Assurance recolte T4',                    '2025-11-28', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',       100.00, 'Frais huilerie pressage',                 '2025-11-22', 'MANUAL', NOW());

-- ==================== DECEMBRE 2025 ====================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      4200.00, 'Grosse vente huile olive marche',         '2025-12-05', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',      1800.00, 'Vente huile olive detail',                '2025-12-15', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       500.00, 'Vente divers produits',                   '2025-12-22', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       700.00, 'Fin cueillette olives',                   '2025-12-03', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   350.00, 'Livraisons clients huile olive',          '2025-12-08', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  180.00, 'Compost oliviers apres recolte',          '2025-12-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   500.00, 'Maintenance annuelle tracteur',           '2025-12-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  130.00, 'Facture eau decembre',                    '2025-12-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LOAN_PAYMENT',450.00, 'Echeance credit agricole decembre',       '2025-12-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'INSURANCE',   150.00, 'Assurance vehicule agricole',             '2025-12-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',       120.00, 'Cadeaux fin annee ouvriers',              '2025-12-25', 'MANUAL', NOW());

-- ==================== JANVIER 2026 ====================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      1800.00, 'Vente stock huile olive restant',         '2026-01-10', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       600.00, 'Vente fourrage hivernal',                 '2026-01-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  300.00, 'Engrais azote pour cereales',             '2026-01-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  160.00, 'Facture eau janvier',                     '2026-01-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       350.00, 'Entretien champs hivernal',               '2026-01-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   280.00, 'Reparation pompe irrigation',             '2026-01-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   100.00, 'Gasoil et deplacement',                   '2026-01-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LOAN_PAYMENT',450.00, 'Echeance credit agricole janvier',        '2026-01-22', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        90.00, 'Frais divers janvier',                    '2026-01-28', 'MANUAL', NOW());

-- ==================== FEVRIER 2026 ====================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      1200.00, 'Vente ble stocke',                        '2026-02-03', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       900.00, 'Vente legumes serre',                     '2026-02-10', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       500.00, 'Vente oeufs et volaille',                 '2026-02-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       500.00, 'Achat semences legumes printemps',        '2026-02-01', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  380.00, 'Engrais phosphate pour cereales',         '2026-02-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  200.00, 'Facture eau fevrier',                     '2026-02-08', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       450.00, 'Ouvriers preparation sol printemps',      '2026-02-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   130.00, 'Transport materiel et semences',          '2026-02-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   150.00, 'Pieces de rechange outils',               '2026-02-14', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        80.00, 'Frais divers fevrier',                    '2026-02-16', 'MANUAL', NOW());

-- ==================== MARS 2026 ====================
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      2500.00, 'Vente primeurs marche gros',              '2026-03-06', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',      1100.00, 'Vente legumes serre mars',                '2026-03-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       450.00, 'Semences ete tomates et poivrons',        '2026-03-02', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  310.00, 'Engrais mars',                            '2026-03-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       500.00, 'Salaires ouvriers mars',                  '2026-03-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  190.00, 'Facture eau mars',                        '2026-03-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   140.00, 'Transport marche mars',                   '2026-03-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LOAN_PAYMENT',450.00, 'Echeance credit agricole mars',           '2026-03-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   220.00, 'Entretien materiel agricole',             '2026-03-22', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        95.00, 'Frais divers mars',                       '2026-03-25', 'MANUAL', NOW());

-- ============================================================
-- PARTIE 3 : SAVINGS TRANSACTIONS
-- Depots - Retraits = 3800 DT
-- ============================================================
INSERT INTO savings_transaction (account_id, type, amount, description, occurred_at, created_at) VALUES
(@ACCOUNT_ID, 'DEPOSIT',    300.00, 'Epargne mensuelle septembre',        '2025-09-06 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    200.00, 'Bonus vente cereales',               '2025-09-19 14:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    250.00, 'Epargne mensuelle octobre',          '2025-10-09 10:00:00', NOW()),
(@ACCOUNT_ID, 'WITHDRAWAL', 150.00, 'Urgence reparation cloture',         '2025-10-16 11:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    500.00, 'Epargne mensuelle novembre',         '2025-11-11 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    800.00, 'Bonus vente huile olive',            '2025-11-21 15:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    600.00, 'Epargne mensuelle decembre',         '2025-12-06 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',   1000.00, 'Gros bonus vente huile en gros',     '2025-12-16 14:00:00', NOW()),
(@ACCOUNT_ID, 'WITHDRAWAL', 400.00, 'Retrait pour maintenance tracteur',  '2025-12-13 09:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    300.00, 'Epargne mensuelle janvier',          '2026-01-11 10:00:00', NOW()),
(@ACCOUNT_ID, 'WITHDRAWAL', 200.00, 'Retrait urgence pompe',              '2026-01-16 11:30:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    350.00, 'Epargne mensuelle fevrier',          '2026-02-04 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    250.00, 'Vente ble epargne',                  '2026-02-11 14:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    500.00, 'Epargne mensuelle mars',             '2026-03-05 10:00:00', NOW()),
(@ACCOUNT_ID, 'WITHDRAWAL', 300.00, 'Achat semences urgence mars',        '2026-03-18 10:00:00', NOW());

-- Mise a jour du solde
-- Depots : 300+200+250+500+800+600+1000+300+350+250+500 = 5050
-- Retraits : 150+400+200+300 = 1050
-- Solde final : 5050 - 1050 = 4000
UPDATE savings_account
SET current_balance = 4000.00
WHERE id = @ACCOUNT_ID;

-- ============================================================
-- PARTIE 4 : BUDGETS
-- ============================================================
INSERT INTO budget (user_id, period_type, period_start, period_end, category, planned_amount, created_at, updated_at) VALUES
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'SEEDS',        500.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'FERTILIZER',   350.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'IRRIGATION',   250.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'LABOR',        600.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'TRANSPORT',    200.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'EQUIPMENT',    300.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'LOAN_PAYMENT', 450.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY',  '2026-03-01', '2026-03-31', 'OTHER',        100.00, NOW(), NOW()),
(@USER_ID, 'SEASONAL', '2026-01-01', '2026-06-30', 'SEEDS',       1500.00, NOW(), NOW()),
(@USER_ID, 'SEASONAL', '2026-01-01', '2026-06-30', 'FERTILIZER',  1200.00, NOW(), NOW()),
(@USER_ID, 'YEARLY',   '2026-01-01', '2026-12-31', 'INSURANCE',    800.00, NOW(), NOW()),
(@USER_ID, 'YEARLY',   '2026-01-01', '2026-12-31', 'EQUIPMENT',   3000.00, NOW(), NOW());

-- ============================================================
-- PARTIE 5 : SAVINGS GOALS
-- Solde = 4000 DT, total targets = 18000 DT
-- Tracteur   : 4000 * (10000/18000) = 2222.22
-- Urgence    : 4000 * (5000/18000)  = 1111.11
-- Irrigation : 4000 * (3000/18000)  =  666.67
-- ============================================================
INSERT INTO savings_goals (id, savings_account_id, goal_name, target_amount, current_amount, target_date, description, achieved, collected, priority, created_at, updated_at) VALUES
(UUID(), @ACCOUNT_ID, 'Achat nouveau tracteur',    10000.00, 2222.22, '2027-06-01', 'Remplacer le tracteur vieillissant',        false, false, 1, NOW(), NOW()),
(UUID(), @ACCOUNT_ID, 'Fonds urgence exploitation', 5000.00, 1111.11, '2026-12-31', 'Reserve de securite pour les imprevus',     false, false, 2, NOW(), NOW()),
(UUID(), @ACCOUNT_ID, 'Systeme irrigation goutte',  3000.00,  666.67, '2026-09-01', 'Moderniser irrigation pour economiser eau', false, false, 3, NOW(), NOW());

-- Sync goalAmount et goalTitle sur le compte
UPDATE savings_account
SET goal_amount = 18000.00,
    goal_title  = '3 objectif(s): Achat nouveau tracteur, Fonds urgence exploitation, Systeme irrigation goutte'
WHERE id = @ACCOUNT_ID;

-- ============================================================
-- VERIFICATION
-- ============================================================
-- SELECT COUNT(*) FROM accounting_entry WHERE user_id = @USER_ID;
-- → Attendu : 72 lignes
--
-- SELECT entry_type, COUNT(*), SUM(amount) FROM accounting_entry WHERE user_id = @USER_ID GROUP BY entry_type;
-- → INCOME  : ~24800 DT
-- → EXPENSE : ~18895 DT
--
-- SELECT current_balance, goal_amount FROM savings_account WHERE id = @ACCOUNT_ID;
-- → current_balance = 4000.00 | goal_amount = 18000.00
--
-- SELECT goal_name, target_amount, current_amount FROM savings_goals WHERE savings_account_id = @ACCOUNT_ID;
-- → 3 goals
--
-- SELECT COUNT(*) FROM budget WHERE user_id = @USER_ID;
-- → Attendu : 12 budgets
-- ============================================================
