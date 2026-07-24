package com.showmethemoney.dashboard.interfaces.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardDailyResponse(
        String yearMonth,
        BigDecimal budgetAmount,
        List<DailyBalancePoint> days
) {}
