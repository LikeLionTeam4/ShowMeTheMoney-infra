package com.showmethemoney.transaction.interfaces.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateTransactionRequest(
        Integer type,
        String categoryCode,
        @Positive BigDecimal amount,
        String memo,
        LocalDate transactionAt
) {}
