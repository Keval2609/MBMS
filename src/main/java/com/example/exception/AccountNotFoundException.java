package com.example.exception;

public class AccountNotFoundException extends BankException {
    public AccountNotFoundException(Integer accountId) {
        super("Account not found with ID: " + accountId);
    }

    public AccountNotFoundException(String message) {
        super(message);
    }
}
