package com.example.exception;

public class AccountFrozenException extends BankException {
    public AccountFrozenException(String message) {
        super(ErrorCode.ACCOUNT_FROZEN, message);
    }

    public AccountFrozenException(String message, Throwable cause) {
        super(ErrorCode.ACCOUNT_FROZEN, message, cause);
    }
}
