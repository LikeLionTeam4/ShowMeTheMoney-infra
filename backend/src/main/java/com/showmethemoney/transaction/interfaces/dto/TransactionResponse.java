package com.showmethemoney.transaction.interfaces.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        String type,
        String categoryCode,
        String categoryName,
        BigDecimal amount,
        String memo,
        LocalDate transactionAt
) {}
