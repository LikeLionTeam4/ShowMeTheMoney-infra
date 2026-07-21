package com.showmethemoney.recurringitem.interfaces.dto;

import java.math.BigDecimal;

public record RecurringItemResponse(
        Long id,
        String type,
        String categoryCode,
        String categoryName,
        String name,
        BigDecimal amount,
        Integer billingDay,
        Integer isActive
) {}
