package com.bankcore.exception;

public class BankCoreException extends RuntimeException {
    public BankCoreException(String message) {
        super(message);
    }

    public BankCoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
