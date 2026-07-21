package com.showmethemoney.transaction.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransactionPageResponse(
        List<TransactionResponse> content,
        long totalElements,
        int totalPages,
        BigDecimal totalIncome,
        BigDecimal totalExpense
) {}
