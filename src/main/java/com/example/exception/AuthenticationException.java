package com.example.exception;

public class AuthenticationException extends BankException {
    public AuthenticationException(String message) {
        super(ErrorCode.AUTHENTICATION_FAILED, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(ErrorCode.AUTHENTICATION_FAILED, message, cause);
    }
}
