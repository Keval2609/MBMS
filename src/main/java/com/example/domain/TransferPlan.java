package com.example.domain;

import java.math.BigDecimal;

public record TransferPlan(
    BigDecimal newFromBalance,
    BigDecimal newToBalance,
    BigDecimal transferAmount
) {}
