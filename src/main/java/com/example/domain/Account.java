package com.example.domain;

import com.example.exception.AccountClosedException;
import com.example.exception.AccountFrozenException;
import com.example.exception.InsufficientFundsException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Account {
    private Integer id;
    private Integer customerId;
    private String accountNumber;
    private Integer branchId;
    private AccountType accountType;
    private BigDecimal balance;
    private AccountStatus status;
    private Integer version; 

    public Account() {
    }

    public Account(Integer id, Integer customerId, String accountNumber, Integer branchId, 
                   AccountType accountType, BigDecimal balance, AccountStatus status, Integer version) {
        this.id = id;
        this.customerId = customerId;
        this.accountNumber = accountNumber;
        this.branchId = branchId;
        this.accountType = accountType;
        this.balance = balance != null ? balance.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.status = status;
        this.version = version;
    }

    // --- GETTERS & SETTERS ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { 
        this.balance = balance != null ? balance.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN); 
    }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public Integer getBranchId() { return branchId; }
    public void setBranchId(Integer branchId) { this.branchId = branchId; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }

    public void assertCanTransact() {
        if (this.status == AccountStatus.FROZEN) {
            throw new AccountFrozenException("Account is frozen: " + this.id);
        }
        if (this.status == AccountStatus.CLOSED) {
            throw new AccountClosedException("Account is closed: " + this.id);
        }
    }

    public void assertSufficientFunds(BigDecimal amount) {
        if (this.balance == null || this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds in account " + this.id + ". Current balance: $" + this.balance
            );
        }
    }
}