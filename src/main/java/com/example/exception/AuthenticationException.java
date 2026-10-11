package com.example.exception;

public class AuthenticationException extends BankException {
    public AuthenticationException(String message) {
        super(message);
    }
}
