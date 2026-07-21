package com.showmethemoney.budget.interfaces.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateBudgetRequest(
        @Positive BigDecimal amount,
        String memo
) {}
