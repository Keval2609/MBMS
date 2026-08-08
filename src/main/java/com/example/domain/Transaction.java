package com.example.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Transaction(
    Integer id,
        String referenceNumber,
        Integer fromAccountId, // Can be null for cash deposits
        Integer toAccountId,   // Can be null for cash withdrawals
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        LocalDateTime timestamp,
        Integer employeeId
) {}
