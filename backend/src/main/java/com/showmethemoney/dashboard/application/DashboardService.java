package com.showmethemoney.dashboard.application;

import com.showmethemoney.budget.domain.Budget;
import com.showmethemoney.budget.infrastructure.BudgetMapper;
import com.showmethemoney.common.YearMonthKey;
import com.showmethemoney.dashboard.infrastructure.CategoryAmountRow;
import com.showmethemoney.dashboard.infrastructure.DailyAmountRow;
import com.showmethemoney.dashboard.infrastructure.DashboardMapper;
import com.showmethemoney.dashboard.interfaces.dto.CategoryExpenseResponse;
import com.showmethemoney.dashboard.interfaces.dto.DailyBalancePoint;
import com.showmethemoney.dashboard.interfaces.dto.DashboardDailyResponse;
import com.showmethemoney.dashboard.interfaces.dto.DashboardSummaryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String dbYearMonth = YearMonthKey.toDbFormat(yearMonth);

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
        String dbYearMonth = YearMonthKey.toDbFormat(yearMonth);
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

    @Transactional(readOnly = true)
    public DashboardDailyResponse getDailyBalances(Long userId, String yearMonth) {
        String dbYearMonth = YearMonthKey.toDbFormat(yearMonth);

        Budget budget = budgetMapper.findByUserIdAndYearMonth(userId, dbYearMonth);
        BigDecimal budgetAmount = budget != null ? budget.getAmount() : null;

        Map<String, BigDecimal> incomeByDate = new HashMap<>();
        Map<String, BigDecimal> expenseByDate = new HashMap<>();
        for (DailyAmountRow row : dashboardMapper.sumDailyAmounts(userId, dbYearMonth)) {
            if (row.getType() == 1) {
                incomeByDate.put(row.getTxDate(), row.getAmount());
            } else {
                expenseByDate.put(row.getTxDate(), row.getAmount());
            }
        }

        YearMonth month = YearMonth.parse(dbYearMonth);
        List<DailyBalancePoint> days = new ArrayList<>();
        BigDecimal cumulativeExpense = BigDecimal.ZERO;
        BigDecimal cumulativeBalance = BigDecimal.ZERO;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            String date = month.atDay(day).toString();
            BigDecimal income = incomeByDate.getOrDefault(date, BigDecimal.ZERO);
            BigDecimal expense = expenseByDate.getOrDefault(date, BigDecimal.ZERO);
            cumulativeExpense = cumulativeExpense.add(expense);
            cumulativeBalance = cumulativeBalance.add(income).subtract(expense);
            days.add(new DailyBalancePoint(date, income, expense, cumulativeExpense, cumulativeBalance));
        }

        return new DashboardDailyResponse(dbYearMonth, budgetAmount, days);
    }
}
