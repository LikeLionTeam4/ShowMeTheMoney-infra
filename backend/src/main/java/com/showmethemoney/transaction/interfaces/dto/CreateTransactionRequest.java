package com.showmethemoney.transaction.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull Integer type,
        @NotBlank String categoryCode,
        @NotNull @Positive BigDecimal amount,
        String memo,
        @NotNull LocalDate transactionAt
) {}
