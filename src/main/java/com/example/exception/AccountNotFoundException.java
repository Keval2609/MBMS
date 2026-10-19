package com.example.exception;

public class AccountNotFoundException extends BankException {
    public AccountNotFoundException(Integer accountId) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, "Account not found with ID: " + accountId);
    }

    public AccountNotFoundException(String message) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, message);
    }
}
