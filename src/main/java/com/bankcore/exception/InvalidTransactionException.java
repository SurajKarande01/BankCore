package com.bankcore.exception;

public class InvalidTransactionException extends BankCoreException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
