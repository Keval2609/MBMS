package com.example.exception;

public class InvalidTransferException extends BankException {
    public InvalidTransferException(String message) {
        super(message);
    }
}
