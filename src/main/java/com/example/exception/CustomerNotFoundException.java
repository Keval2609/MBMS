package com.example.exception;

public class CustomerNotFoundException extends BankException {
    public CustomerNotFoundException(Integer customerId) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, "Customer not found with ID: " + customerId);
    }

    public CustomerNotFoundException(String message) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, message);
    }
}
