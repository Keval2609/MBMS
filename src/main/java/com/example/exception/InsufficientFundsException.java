package com.example.exception;

public class InsufficientFundsException extends BankException {
    public InsufficientFundsException(String message) {
        super(ErrorCode.INSUFFICIENT_FUNDS, message);
    }

    public InsufficientFundsException(String message, Throwable cause) {
        super(ErrorCode.INSUFFICIENT_FUNDS, message, cause);
    }
}
