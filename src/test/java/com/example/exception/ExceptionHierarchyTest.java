package com.example.exception;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class ExceptionHierarchyTest {

    @Test
    public void testBankExceptionHierarchy() {
        BankException ex1 = new InsufficientFundsException("Insufficient balance");
        BankException ex2 = new AccountNotFoundException(101);
        BankException ex3 = new AccountFrozenException("Account is frozen: 101");
        BankException ex4 = new AccountClosedException("Account is closed: 101");
        BankException ex5 = new UnauthorizedAccessException("Forbidden");
        BankException ex6 = new InvalidTransferException("Cannot transfer to self");
        BankException ex7 = new AuthenticationException("Invalid username or password");
        BankException ex8 = new CustomerNotFoundException(202);

        assertTrue(ex1 instanceof RuntimeException);
        assertEquals("Account not found with ID: 101", ex2.getMessage());
        assertEquals("Account is frozen: 101", ex3.getMessage());
        assertEquals("Account is closed: 101", ex4.getMessage());
        assertEquals("Forbidden", ex5.getMessage());
        assertEquals("Cannot transfer to self", ex6.getMessage());
        assertEquals("Invalid username or password", ex7.getMessage());
        assertEquals("Customer not found with ID: 202", ex8.getMessage());

        assertEquals(ErrorCode.INSUFFICIENT_FUNDS, ex1.getErrorCode());
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, ex2.getErrorCode());
        assertEquals(ErrorCode.ACCOUNT_FROZEN, ex3.getErrorCode());
        assertEquals(ErrorCode.ACCOUNT_CLOSED, ex4.getErrorCode());
        assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, ex5.getErrorCode());
        assertEquals(ErrorCode.INVALID_TRANSFER, ex6.getErrorCode());
        assertEquals(ErrorCode.AUTHENTICATION_FAILED, ex7.getErrorCode());
        assertEquals(ErrorCode.CUSTOMER_NOT_FOUND, ex8.getErrorCode());
    }

    @Test
    public void testDataAccessException() {
        SQLException sqlEx = new SQLException("Connection timeout");
        DataAccessException ex = new DataAccessException("Failed query", sqlEx);

        assertTrue(ex instanceof BankException);
        assertTrue(ex instanceof RuntimeException);
        assertEquals("Failed query", ex.getMessage());
        assertSame(sqlEx, ex.getCause());
        assertEquals(ErrorCode.DATA_ACCESS_ERROR, ex.getErrorCode());
    }
}
