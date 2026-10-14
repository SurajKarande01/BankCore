package com.bankcore.exception;

public class InsufficientBalanceException extends BankCoreException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
