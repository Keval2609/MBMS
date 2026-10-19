package com.example.exception;

public class DataAccessException extends BankException {
    public DataAccessException(String message) {
        super(ErrorCode.DATA_ACCESS_ERROR, message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(ErrorCode.DATA_ACCESS_ERROR, message, cause);
    }
}
