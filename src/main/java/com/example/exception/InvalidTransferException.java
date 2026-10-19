package com.example.exception;

public class InvalidTransferException extends BankException {
    public InvalidTransferException(String message) {
        super(ErrorCode.INVALID_TRANSFER, message);
    }

    public InvalidTransferException(String message, Throwable cause) {
        super(ErrorCode.INVALID_TRANSFER, message, cause);
    }
}
