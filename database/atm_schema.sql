CREATE DATABASE IF NOT EXISTS atm_simulator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE atm_simulator;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS transactions;
DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS customers;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE customers (
    customer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NULL,
    phone VARCHAR(30) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (customer_id),
    UNIQUE KEY uq_customers_email (email)
) ENGINE=InnoDB;

CREATE TABLE accounts (
    account_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_id BIGINT UNSIGNED NOT NULL,
    card_number CHAR(16) NOT NULL,
    pin_hash CHAR(64) NOT NULL,
    pin_salt VARCHAR(64) NOT NULL,
    failed_attempts INT UNSIGNED NOT NULL DEFAULT 0,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status ENUM('ACTIVE','BLOCKED','CLOSED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (account_id),
    UNIQUE KEY uq_accounts_card_number (card_number),
    KEY idx_accounts_customer (customer_id),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    CONSTRAINT chk_accounts_balance CHECK (balance >= 0)
) ENGINE=InnoDB;

CREATE TABLE transactions (
    transaction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id BIGINT UNSIGNED NOT NULL,
    transaction_type ENUM('DEPOSIT','WITHDRAWAL') NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    reference_code VARCHAR(32) NOT NULL,
    transaction_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id),
    UNIQUE KEY uq_transactions_reference (reference_code),
    KEY idx_transactions_account_time (account_id, transaction_time),
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_balance CHECK (balance_after >= 0)
) ENGINE=InnoDB;

-- Demo Customers
INSERT INTO customers (customer_id, full_name, email, phone) VALUES
(1, 'Aarav Sharma', 'aarav@example.com', '+91-9000000001'),
(2, 'Priya Patel', 'priya@example.com', '+91-9888888888');

-- Demo Accounts with cryptographic salts
-- Demo Account 1 (Easy): Card 1234123412341234, PIN 1234, Salt: 9f8e7d6c5b4a3120
-- Demo Account 2 (Easy): Card 4321432143214321, PIN 4321, Salt: 1a2b3c4d5e6f7089
-- Demo Account 3 (Legacy): Card 5000123412341234, PIN 1234, Salt: 9f8e7d6c5b4a3120
INSERT INTO accounts (account_id, customer_id, card_number, pin_hash, pin_salt, failed_attempts, balance, status) VALUES
(1, 1, '1234123412341234', SHA2(CONCAT('9f8e7d6c5b4a3120', '1234'), 256), '9f8e7d6c5b4a3120', 0, 50000.00, 'ACTIVE'),
(2, 2, '4321432143214321', SHA2(CONCAT('1a2b3c4d5e6f7089', '4321'), 256), '1a2b3c4d5e6f7089', 0, 25000.00, 'ACTIVE'),
(3, 1, '5000123412341234', SHA2(CONCAT('9f8e7d6c5b4a3120', '1234'), 256), '9f8e7d6c5b4a3120', 0, 50000.00, 'ACTIVE');

INSERT INTO transactions (account_id, transaction_type, amount, balance_after, reference_code, transaction_time) VALUES
(1, 'DEPOSIT', 50000.00, 50000.00, 'SEED-0000000001', CURRENT_TIMESTAMP - INTERVAL 2 DAY),
(2, 'DEPOSIT', 25000.00, 25000.00, 'SEED-0000000002', CURRENT_TIMESTAMP - INTERVAL 1 DAY),
(3, 'DEPOSIT', 50000.00, 50000.00, 'SEED-0000000003', CURRENT_TIMESTAMP - INTERVAL 2 DAY);
