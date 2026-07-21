package com.showmethemoney.recurringitem.interfaces.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateRecurringItemRequest(
        String name,
        @Positive BigDecimal amount,
        @Min(1) @Max(31) Integer billingDay,
        String categoryCode,
        Boolean isActive
) {}
