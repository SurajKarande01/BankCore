package com.bankcore.exception;

public class AccountNotFoundException extends BankCoreException {
    public AccountNotFoundException(String message) {
        super(message);
    }

    public static AccountNotFoundException forAccountNumber(String accountNumber) {
        return new AccountNotFoundException("Account not found with account number: " + accountNumber);
    }

    public static AccountNotFoundException forId(Long id) {
        return new AccountNotFoundException("Account not found with ID: " + id);
    }
}
