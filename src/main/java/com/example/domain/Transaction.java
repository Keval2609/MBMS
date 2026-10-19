package com.example.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public class Transaction {
    private Integer id;
    private String referenceNumber;
    private Integer fromAccountId;
    private Integer toAccountId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private Integer employeeId;
    private Instant createdAt;

    public Transaction() {}

    public Transaction(Integer id, String referenceNumber, Integer fromAccountId, Integer toAccountId,
                       BigDecimal amount, TransactionType type, TransactionStatus status,
                       Integer employeeId, Instant createdAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        this.type = type;
        this.status = status;
        this.employeeId = employeeId;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public Integer getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Integer fromAccountId) { this.fromAccountId = fromAccountId; }

    public Integer getToAccountId() { return toAccountId; }
    public void setToAccountId(Integer toAccountId) { this.toAccountId = toAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_EVEN) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
    }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}