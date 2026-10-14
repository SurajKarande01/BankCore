package com.bankcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BankCore - Enterprise Banking Transaction and Account Ledger Backend System
 * 
 * Features:
 * - High-integrity Account Management (Savings & Current)
 * - ACID-compliant fund transfers using Spring @Transactional
 * - Row-level Pessimistic Locking to prevent race conditions & lost updates
 * - Immutable Audit Trail capturing actor and operations
 * - Monthly Statement Generation with opening and closing balance aggregation
 * 
 * @author Suraj Daryappa Karande
 */
@SpringBootApplication
public class BankCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCoreApplication.class, args);
    }
}
