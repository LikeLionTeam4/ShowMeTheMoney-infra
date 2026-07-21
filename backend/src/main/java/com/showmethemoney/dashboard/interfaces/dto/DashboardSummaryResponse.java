package com.showmethemoney.dashboard.interfaces.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        String yearMonth,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        BigDecimal budgetAmount,
        Double budgetUsageRate,
        Boolean isOverBudget
) {}
