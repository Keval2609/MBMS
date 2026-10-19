package com.example.exception;

public class UnauthorizedAccessException extends BankException {
    public UnauthorizedAccessException(String message) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, message);
    }

    public UnauthorizedAccessException(String message, Throwable cause) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, message, cause);
    }
}
