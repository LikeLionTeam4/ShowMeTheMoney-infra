package com.showmethemoney.budget.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

// yearMonth: "202606" 형태로 수신, DB 저장 시 "2026-06" 으로 변환
public record CreateBudgetRequest(
        @NotBlank String yearMonth,
        @NotNull @Positive BigDecimal amount,
        String memo
) {}
