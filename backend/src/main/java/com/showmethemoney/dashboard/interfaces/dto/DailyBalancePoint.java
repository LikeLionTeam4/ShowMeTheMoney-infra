package com.showmethemoney.dashboard.interfaces.dto;

import java.math.BigDecimal;

public record DailyBalancePoint(
        String date,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal cumulativeExpense,
        BigDecimal cumulativeBalance
) {}
