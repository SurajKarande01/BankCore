package com.bankcore.exception;

public class AccountInactiveException extends BankCoreException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
