package com.example.exception;

public class UnauthorizedAccessException extends BankException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
