package com.showmethemoney.dashboard.application;

import com.showmethemoney.budget.domain.Budget;
import com.showmethemoney.budget.infrastructure.BudgetMapper;
import com.showmethemoney.common.BusinessException;
import com.showmethemoney.common.ErrorCode;
import com.showmethemoney.dashboard.infrastructure.CategoryAmountRow;
import com.showmethemoney.dashboard.infrastructure.DashboardMapper;
import com.showmethemoney.dashboard.interfaces.dto.CategoryExpenseResponse;
import com.showmethemoney.dashboard.interfaces.dto.DashboardSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final BudgetMapper budgetMapper;

    public DashboardService(DashboardMapper dashboardMapper, BudgetMapper budgetMapper) {
        this.dashboardMapper = dashboardMapper;
        this.budgetMapper = budgetMapper;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId, String yearMonth) {
        String dbYearMonth = toDbYearMonth(yearMonth);

        BigDecimal totalIncome = dashboardMapper.sumAmountByType(userId, dbYearMonth, 1);
        BigDecimal totalExpense = dashboardMapper.sumAmountByType(userId, dbYearMonth, 0);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        Budget budget = budgetMapper.findByUserIdAndYearMonth(userId, dbYearMonth);
        BigDecimal budgetAmount = budget != null ? budget.getAmount() : null;

        Double usageRate = null;
        Boolean isOverBudget = false;
        if (budgetAmount != null && budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            usageRate = totalExpense.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
            isOverBudget = totalExpense.compareTo(budgetAmount) > 0;
        }

        return new DashboardSummaryResponse(dbYearMonth, totalIncome, totalExpense, balance, budgetAmount, usageRate, isOverBudget);
    }

    @Transactional(readOnly = true)
    public List<CategoryExpenseResponse> getCategoryExpenses(Long userId, String yearMonth, Integer type) {
        String dbYearMonth = toDbYearMonth(yearMonth);
        int resolvedType = type != null ? type : 0;

        List<CategoryAmountRow> rows = dashboardMapper.sumByCategory(userId, dbYearMonth, resolvedType);
        BigDecimal total = rows.stream().map(CategoryAmountRow::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return rows.stream()
                .map(row -> {
                    double percentage = total.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : row.getAmount().divide(total, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue();
                    return new CategoryExpenseResponse(row.getCategoryCode(), row.getCategoryName(), row.getAmount(), percentage);
                })
                .toList();
    }

    // "202606" → "2026-06"
    private String toDbYearMonth(String ym) {
        if (ym == null || ym.length() != 6) throw new BusinessException(ErrorCode.INVALID_INPUT);
        return ym.substring(0, 4) + "-" + ym.substring(4);
    }
}
