package com.showmethemoney.dashboard.interfaces.dto;

import java.math.BigDecimal;

public record CategoryExpenseResponse(
        String categoryCode,
        String categoryName,
        BigDecimal amount,
        Double ratio
) {}
