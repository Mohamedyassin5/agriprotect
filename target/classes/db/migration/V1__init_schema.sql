-- AgriProtect Finance Platform - Initial Schema
-- MySQL Database Migration V1

-- =====================================================
-- USER TABLE
-- =====================================================
CREATE TABLE `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(150) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(200) NOT NULL,
    `phone` VARCHAR(20),
    `region` VARCHAR(100),
    `role` ENUM('FARMER', 'ADMIN') NOT NULL DEFAULT 'FARMER',
    `agri_score` INT NOT NULL DEFAULT 50,
    `risk_level` ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL DEFAULT 'MEDIUM',
    `status` ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `chk_agri_score` CHECK (`agri_score` >= 0 AND `agri_score` <= 100),
    INDEX `idx_user_email` (`email`),
    INDEX `idx_user_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SAVINGS MODULE TABLES
-- =====================================================

-- Savings Account (one per user)
CREATE TABLE `savings_account` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL UNIQUE,
    `current_balance` DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    `goal_amount` DECIMAL(15,2),
    `goal_title` VARCHAR(120),
    `status` ENUM('ACTIVE', 'SUSPENDED', 'CLOSED') NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_savings_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    INDEX `idx_savings_user_id` (`user_id`),
    INDEX `idx_savings_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Savings Transactions
CREATE TABLE `savings_transaction` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `account_id` BIGINT NOT NULL,
    `type` ENUM('DEPOSIT', 'WITHDRAWAL') NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `description` VARCHAR(255),
    `occurred_at` DATETIME NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_transaction_account` FOREIGN KEY (`account_id`) REFERENCES `savings_account`(`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_amount_positive` CHECK (`amount` > 0),
    INDEX `idx_savings_tx_account_date` (`account_id`, `occurred_at` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- ACCOUNTING MODULE TABLES
-- =====================================================

-- Accounting Entries
CREATE TABLE `accounting_entry` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `entry_type` ENUM('INCOME', 'EXPENSE') NOT NULL,
    `category` ENUM('SALES', 'SEEDS', 'FERTILIZER', 'IRRIGATION', 'LABOR', 'TRANSPORT', 'EQUIPMENT', 'INSURANCE', 'LOAN_PAYMENT', 'OTHER') NOT NULL,
    `amount` DECIMAL(15,2) NOT NULL,
    `description` VARCHAR(255),
    `entry_date` DATE NOT NULL,
    `source` ENUM('MANUAL', 'SAVINGS', 'CREDIT', 'INSURANCE') NOT NULL DEFAULT 'MANUAL',
    `source_ref_id` BIGINT,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_entry_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_entry_amount_positive` CHECK (`amount` > 0),
    INDEX `idx_entry_user_date` (`user_id`, `entry_date` DESC),
    INDEX `idx_entry_type` (`entry_type`),
    INDEX `idx_entry_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Budgets
CREATE TABLE `budget` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NOT NULL,
    `period_type` ENUM('MONTHLY', 'SEASONAL', 'YEARLY') NOT NULL,
    `period_start` DATE NOT NULL,
    `period_end` DATE NOT NULL,
    `category` ENUM('SALES', 'SEEDS', 'FERTILIZER', 'IRRIGATION', 'LABOR', 'TRANSPORT', 'EQUIPMENT', 'INSURANCE', 'LOAN_PAYMENT', 'OTHER') NOT NULL,
    `planned_amount` DECIMAL(15,2) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_budget_user` FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    CONSTRAINT `chk_planned_amount_positive` CHECK (`planned_amount` > 0),
    CONSTRAINT `chk_period_dates` CHECK (`period_end` >= `period_start`),
    INDEX `idx_budget_user_period` (`user_id`, `period_start`, `period_end`),
    INDEX `idx_budget_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
