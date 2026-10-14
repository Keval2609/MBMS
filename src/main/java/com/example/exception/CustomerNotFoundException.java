package com.example.exception;

public class CustomerNotFoundException extends BankException {
    public CustomerNotFoundException(Integer customerId) {
        super("Customer not found with ID: " + customerId);
    }

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
