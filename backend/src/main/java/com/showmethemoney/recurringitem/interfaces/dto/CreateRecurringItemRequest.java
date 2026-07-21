package com.showmethemoney.recurringitem.interfaces.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateRecurringItemRequest(
        @NotNull Integer type,
        @NotBlank String categoryCode,
        @NotBlank String name,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Min(1) @Max(31) Integer billingDay
) {}
