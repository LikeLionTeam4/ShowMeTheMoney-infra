package com.showmethemoney.budget.interfaces.dto;

import java.math.BigDecimal;

public record BudgetResponse(Long id, String yearMonth, BigDecimal amount) {}
