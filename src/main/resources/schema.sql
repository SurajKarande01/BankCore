-- ==========================================================
-- BankCore Banking Transaction & Account Ledger System Schema
-- Compatible with MySQL 8.0+ and H2 Database Engine
-- ==========================================================

-- 1. Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(32) NOT NULL UNIQUE,
    account_holder_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone_number VARCHAR(20) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    account_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

-- 2. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_reference VARCHAR(64) NOT NULL UNIQUE,
    source_account_id BIGINT NULL,
    source_account_number VARCHAR(32) NULL,
    destination_account_id BIGINT NULL,
    destination_account_number VARCHAR(32) NULL,
    amount DECIMAL(15, 2) NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    description VARCHAR(255) NULL,
    initiated_by VARCHAR(100) NULL,
    source_balance_after DECIMAL(15, 2) NULL,
    destination_balance_after DECIMAL(15, 2) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_txn_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(id) ON DELETE SET NULL,
    CONSTRAINT fk_txn_dest_account FOREIGN KEY (destination_account_id) REFERENCES accounts(id) ON DELETE SET NULL
);

-- 3. Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    triggered_by VARCHAR(100) NOT NULL,
    details TEXT NULL,
    status VARCHAR(20) NOT NULL,
    ip_address VARCHAR(50) NULL,
    created_at DATETIME NOT NULL
);

-- ==========================================================
-- Performance & Financial Query Indexes
-- ==========================================================

CREATE INDEX IF NOT EXISTS idx_accounts_acc_number ON accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_txn_reference ON transactions(transaction_reference);
CREATE INDEX IF NOT EXISTS idx_txn_source_date ON transactions(source_account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_txn_dest_date ON transactions(destination_account_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_name, entity_id);
