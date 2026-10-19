package com.example.exception;

public class AccountClosedException extends BankException {
    public AccountClosedException(String message) {
        super(ErrorCode.ACCOUNT_CLOSED, message);
    }

    public AccountClosedException(String message, Throwable cause) {
        super(ErrorCode.ACCOUNT_CLOSED, message, cause);
    }
}
