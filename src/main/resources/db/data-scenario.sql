-- ============================================================
-- AgriProtect — Scenario de test complet (6 mois)
-- Agriculteur tunisien : cereales + oliviers
-- Periode : Septembre 2025 → Fevrier 2026
-- ============================================================
--
-- COMMENT CA MARCHE (nouveau systeme Goals ↔ Savings) :
--
--   1. On cree un Savings Account VIDE (pas de goalAmount/goalTitle)
--   2. On cree des Goals → le compte se met a jour automatiquement :
--      - goalAmount = somme de tous les targetAmount des goals actifs
--      - goalTitle  = resume des noms de goals ("3 objectif(s): Tracteur, Irrigation, ...")
--   3. Chaque depot/retrait redistribue le solde proportionnellement aux goals :
--      - Goal A (target 10000, 55.5%) recoit 55.5% du solde
--      - Goal B (target 5000, 27.8%)  recoit 27.8% du solde
--      - Goal C (target 3000, 16.7%)  recoit 16.7% du solde
--   4. Quand un goal atteint sa cible → auto-marque achieved = true
--
-- ============================================================
-- INSTRUCTIONS :
-- 1. REMPLACEZ 'YOUR_USER_ID' ci-dessous par votre vrai user ID (UUID)
--    → SELECT id FROM users WHERE email = 'votre@email.com';
-- 2. Executez ce script dans phpMyAdmin (onglet SQL) ou MySQL terminal
-- 3. Le script supprime TOUT (savings, goals, budgets, entries) et recree proprement
-- 4. Le savings_account est recree automatiquement avec le bon ID
-- ============================================================

SET @USER_ID = '70bde437-99a0-4d03-a23f-34330b49608f';

-- ============================================================
-- PARTIE 0 : NETTOYAGE COMPLET (supprime TOUT sauf users et crops)
-- ============================================================
-- Ordre important : respecter les foreign keys
-- On supprime TOUTES les donnees pour repartir proprement

DELETE FROM savings_goals;
DELETE FROM savings_transaction;
DELETE FROM savings_account;
DELETE FROM budget;
DELETE FROM accounting_entry;

-- Recreer le compte epargne proprement (solde a 0, pas de goals)
INSERT INTO savings_account (user_id, account_name, current_balance, monthly_savings_target, goal_amount, goal_title, status, created_at, updated_at)
VALUES (@USER_ID, 'Epargne exploitation agricole', 0.00, 500.00, NULL, NULL, 'ACTIVE', NOW(), NOW());

-- Recuperer l'ID du compte qu'on vient de creer
SET @ACCOUNT_ID = LAST_INSERT_ID();

-- ============================================================
-- PARTIE 1 : ACCOUNTING ENTRIES (Revenus & Depenses)
-- ============================================================
-- Histoire : Un agriculteur qui cultive des cereales et des oliviers.
-- Saison de recolte cereales = Juin-Aout (revenus SALES)
-- Saison olives = Novembre-Janvier (revenus SALES)
-- Depenses regulieres tout au long de l'annee

-- ==================== SEPTEMBRE 2025 ====================
-- Debut de saison : achat de semences et preparation du sol
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      2200.00, 'Vente reste cereales ete 2025',          '2025-09-05', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       800.00, 'Vente legumes potager',                  '2025-09-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       650.00, 'Achat semences ble dur saison 2025-2026','2025-09-03', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       280.00, 'Semences orge',                          '2025-09-04', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  420.00, 'Engrais NPK preparation sol',            '2025-09-08', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   350.00, 'Reparation tracteur',                    '2025-09-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       400.00, 'Main d oeuvre labourage',                '2025-09-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  180.00, 'Facture eau irrigation septembre',       '2025-09-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   120.00, 'Gasoil tracteur + transport',            '2025-09-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'INSURANCE',   200.00, 'Assurance recolte trimestrielle',        '2025-09-25', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        85.00, 'Frais divers (telephone, fournitures)',  '2025-09-28', 'MANUAL', NOW());

-- ==================== OCTOBRE 2025 ====================
-- Semis en cours, arrosage regulier
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      1500.00, 'Vente legumes marche local',             '2025-10-08', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       600.00, 'Vente fourrage',                         '2025-10-22', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       180.00, 'Semences complementaires',               '2025-10-02', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  350.00, 'Amendement calcaire + fumier',           '2025-10-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  220.00, 'Facture eau irrigation octobre',         '2025-10-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       500.00, 'Journaliers pour semis',                 '2025-10-14', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   150.00, 'Transport semences et materiel',         '2025-10-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   200.00, 'Achat pieces systeme irrigation',        '2025-10-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LOAN_PAYMENT',450.00, 'Echeance credit agricole octobre',       '2025-10-25', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        70.00, 'Frais divers',                           '2025-10-28', 'MANUAL', NOW());

-- ==================== NOVEMBRE 2025 ====================
-- Debut recolte olives → revenus en hausse
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      3500.00, 'Premiere vente huile olive',             '2025-11-10', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',      1200.00, 'Vente olives de table',                  '2025-11-20', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       400.00, 'Vente legumes',                          '2025-11-25', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       800.00, 'Equipe cueillette olives (10 jours)',    '2025-11-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       600.00, 'Journaliers cueillette suite',           '2025-11-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   300.00, 'Transport olives vers huilerie',         '2025-11-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   250.00, 'Location materiel cueillette',           '2025-11-04', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  200.00, 'Engrais entretien oliviers',             '2025-11-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  150.00, 'Facture eau novembre',                   '2025-11-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'INSURANCE',   200.00, 'Assurance recolte T4',                   '2025-11-28', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',       100.00, 'Frais huilerie (pressage)',              '2025-11-22', 'MANUAL', NOW());

-- ==================== DECEMBRE 2025 ====================
-- Suite recolte olives, ventes importantes
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      4200.00, 'Grosse vente huile olive marche',        '2025-12-05', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',      1800.00, 'Vente huile olive detail',               '2025-12-15', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       500.00, 'Vente divers produits',                  '2025-12-22', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       700.00, 'Fin cueillette olives',                  '2025-12-03', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   350.00, 'Livraisons clients huile olive',         '2025-12-08', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  180.00, 'Compost pour oliviers apres recolte',    '2025-12-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   500.00, 'Maintenance annuelle tracteur',          '2025-12-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  130.00, 'Facture eau decembre',                   '2025-12-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LOAN_PAYMENT',450.00, 'Echeance credit agricole decembre',      '2025-12-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'INSURANCE',   150.00, 'Assurance vehicule agricole',            '2025-12-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',       120.00, 'Cadeaux fin annee ouvriers + divers',    '2025-12-25', 'MANUAL', NOW());

-- ==================== JANVIER 2026 ====================
-- Saison calme, entretien hivernal
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      1800.00, 'Vente stock huile olive restant',        '2026-01-10', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       600.00, 'Vente fourrage hivernal',                '2026-01-20', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  300.00, 'Engrais azote pour cereales',            '2026-01-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  160.00, 'Facture eau janvier',                    '2026-01-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       350.00, 'Entretien champs hivernal',              '2026-01-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   280.00, 'Reparation pompe irrigation',            '2026-01-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   100.00, 'Gasoil et deplacement',                  '2026-01-18', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LOAN_PAYMENT',450.00, 'Echeance credit agricole janvier',       '2026-01-22', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        90.00, 'Frais divers janvier',                   '2026-01-28', 'MANUAL', NOW());

-- ==================== FEVRIER 2026 (mois courant) ====================
-- Preparation nouvelle saison
INSERT INTO accounting_entry (user_id, entry_type, category, amount, description, entry_date, source, created_at) VALUES
(@USER_ID, 'INCOME',  'SALES',      1200.00, 'Vente ble stocke',                       '2026-02-03', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       900.00, 'Vente legumes serre',                    '2026-02-10', 'MANUAL', NOW()),
(@USER_ID, 'INCOME',  'SALES',       500.00, 'Vente oeufs et volaille',                '2026-02-15', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'SEEDS',       500.00, 'Achat semences legumes printemps',       '2026-02-01', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'FERTILIZER',  380.00, 'Engrais phosphate pour cereales',        '2026-02-05', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'IRRIGATION',  200.00, 'Facture eau fevrier',                    '2026-02-08', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'LABOR',       450.00, 'Ouvriers preparation sol printemps',     '2026-02-10', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'TRANSPORT',   130.00, 'Transport materiel et semences',         '2026-02-12', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'EQUIPMENT',   150.00, 'Pieces de rechange outils',              '2026-02-14', 'MANUAL', NOW()),
(@USER_ID, 'EXPENSE', 'OTHER',        80.00, 'Frais divers fevrier',                   '2026-02-16', 'MANUAL', NOW());


-- ============================================================
-- PARTIE 2 : SAVINGS TRANSACTIONS (Depots & Retraits)
-- ============================================================
-- Strategie : depot regulier + depots supplementaires apres bonnes ventes
-- Retraits occasionnels pour urgences

INSERT INTO savings_transaction (account_id, type, amount, description, occurred_at, created_at) VALUES
-- Septembre 2025
(@ACCOUNT_ID, 'DEPOSIT',    300.00, 'Epargne mensuelle septembre',       '2025-09-06 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    200.00, 'Bonus vente cereales',              '2025-09-19 14:00:00', NOW()),

-- Octobre 2025
(@ACCOUNT_ID, 'DEPOSIT',    250.00, 'Epargne mensuelle octobre',         '2025-10-09 10:00:00', NOW()),
(@ACCOUNT_ID, 'WITHDRAWAL', 150.00, 'Urgence reparation cloture',        '2025-10-16 11:00:00', NOW()),

-- Novembre 2025 — bonne recolte olives → depots plus eleves
(@ACCOUNT_ID, 'DEPOSIT',    500.00, 'Epargne mensuelle novembre',        '2025-11-11 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    800.00, 'Bonus vente huile olive',           '2025-11-21 15:00:00', NOW()),

-- Decembre 2025 — tres bonne periode
(@ACCOUNT_ID, 'DEPOSIT',    600.00, 'Epargne mensuelle decembre',        '2025-12-06 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',   1000.00, 'Gros bonus vente huile en gros',    '2025-12-16 14:00:00', NOW()),
(@ACCOUNT_ID, 'WITHDRAWAL', 400.00, 'Retrait pour maintenance tracteur',  '2025-12-13 09:00:00', NOW()),

-- Janvier 2026 — mois calme
(@ACCOUNT_ID, 'DEPOSIT',    300.00, 'Epargne mensuelle janvier',         '2026-01-11 10:00:00', NOW()),
(@ACCOUNT_ID, 'WITHDRAWAL', 200.00, 'Retrait urgence pompe',             '2026-01-16 11:30:00', NOW()),

-- Fevrier 2026 — mois courant
(@ACCOUNT_ID, 'DEPOSIT',    350.00, 'Epargne mensuelle fevrier',         '2026-02-04 10:00:00', NOW()),
(@ACCOUNT_ID, 'DEPOSIT',    250.00, 'Vente ble → epargne',              '2026-02-11 14:00:00', NOW());


-- ============================================================
-- PARTIE 3 : MISE A JOUR DU SOLDE SAVINGS ACCOUNT
-- ============================================================
-- Calcul : Depots(300+200+250+500+800+600+1000+300+350+250) - Retraits(150+400+200) = 4550 - 750 = 3800
UPDATE savings_account
SET current_balance = 3800.00
WHERE id = @ACCOUNT_ID;
-- NOTE : goalAmount et goalTitle ne sont PAS mis a jour ici.
-- Ils seront auto-calcules quand on creera les goals (Partie 5).


-- ============================================================
-- PARTIE 4 : BUDGETS (mois courant — Fevrier 2026)
-- ============================================================
INSERT INTO budget (user_id, period_type, period_start, period_end, category, planned_amount, created_at, updated_at) VALUES
(@USER_ID, 'MONTHLY', '2026-02-01', '2026-02-28', 'SEEDS',        600.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY', '2026-02-01', '2026-02-28', 'FERTILIZER',   400.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY', '2026-02-01', '2026-02-28', 'IRRIGATION',   250.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY', '2026-02-01', '2026-02-28', 'LABOR',        500.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY', '2026-02-01', '2026-02-28', 'TRANSPORT',    200.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY', '2026-02-01', '2026-02-28', 'EQUIPMENT',    200.00, NOW(), NOW()),
(@USER_ID, 'MONTHLY', '2026-02-01', '2026-02-28', 'OTHER',        100.00, NOW(), NOW());


-- ============================================================
-- PARTIE 5 : SAVINGS GOALS (auto-sync avec le solde)
-- ============================================================
-- IMPORTANT : Comme on insere directement en SQL (pas via l'API),
-- le sync automatique ne se declenche pas. On doit calculer currentAmount manuellement.
--
-- Solde = 3800 DT, totalTarget = 10000 + 5000 + 3000 = 18000
-- Tracteur   : 3800 * (10000/18000) = 2111.11
-- Urgence    : 3800 * (5000/18000)  = 1055.56
-- Irrigation : 3800 * (3000/18000)  =  633.33

INSERT INTO savings_goals (id, savings_account_id, goal_name, target_amount, current_amount, target_date, description, achieved, created_at, updated_at) VALUES
(UUID(), @ACCOUNT_ID, 'Achat nouveau tracteur',     10000.00, 2111.11, '2027-06-01', 'Objectif principal : remplacer le tracteur vieillissant',  false, NOW(), NOW()),
(UUID(), @ACCOUNT_ID, 'Fonds urgence exploitation',  5000.00, 1055.56, '2026-12-31', 'Constituer un fonds de securite pour les imprevus',        false, NOW(), NOW()),
(UUID(), @ACCOUNT_ID, 'Systeme irrigation goutte',   3000.00,  633.33, '2026-09-01', 'Moderniser l irrigation pour economiser l eau',            false, NOW(), NOW());

-- Mettre a jour le compte avec les infos auto-calculees des goals
UPDATE savings_account
SET goal_amount = 18000.00,
    goal_title = '3 objectif(s): Achat nouveau tracteur, Fonds urgence exploitation, Systeme irrigation goutte'
WHERE id = @ACCOUNT_ID;


-- ============================================================
-- VERIFICATION : Executez ces requetes pour verifier
-- ============================================================
-- SELECT COUNT(*) AS total_entries FROM accounting_entry WHERE user_id = @USER_ID;
-- → Attendu : 62 lignes
--
-- SELECT entry_type, COUNT(*), SUM(amount) FROM accounting_entry WHERE user_id = @USER_ID GROUP BY entry_type;
-- → INCOME : 16 lignes, ~22200 DT
-- → EXPENSE : 46 lignes, ~14675 DT
--
-- SELECT COUNT(*) AS total_transactions FROM savings_transaction WHERE account_id = @ACCOUNT_ID;
-- → Attendu : 13 transactions
--
-- SELECT current_balance, goal_amount, goal_title FROM savings_account WHERE id = @ACCOUNT_ID;
-- → current_balance = 3800.00
-- → goal_amount = 18000.00
-- → goal_title = "3 objectif(s): Achat nouveau tracteur, Fonds urgence exploitation, Systeme irrigation goutte"
--
-- SELECT goal_name, target_amount, current_amount, achieved FROM savings_goals WHERE savings_account_id = @ACCOUNT_ID;
-- → Tracteur   : 10000 / 2111.11 / false
-- → Urgence    : 5000  / 1055.56 / false
-- → Irrigation : 3000  /  633.33 / false
--
-- SELECT COUNT(*) AS total_budgets FROM budget WHERE user_id = @USER_ID;
-- → Attendu : 7 budgets
--
-- ============================================================
-- APRES L'EXECUTION DE CE SCRIPT :
-- Testez le sync automatique via l'API :
--
-- 1. POST /api/savings/transactions → { "type": "DEPOSIT", "amount": 1000 }
-- 2. GET /api/savings/goals → verifier que currentAmount a augmente proportionnellement
-- 3. GET /api/savings/accounts/me → verifier goalAmount=18000, goalTitle auto-genere
-- ============================================================
