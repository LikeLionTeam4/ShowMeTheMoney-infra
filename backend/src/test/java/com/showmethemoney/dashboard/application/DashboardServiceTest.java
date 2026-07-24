package com.showmethemoney.dashboard.application;

import com.showmethemoney.budget.domain.Budget;
import com.showmethemoney.budget.infrastructure.BudgetMapper;
import com.showmethemoney.dashboard.infrastructure.DailyAmountRow;
import com.showmethemoney.dashboard.infrastructure.DashboardMapper;
import com.showmethemoney.dashboard.interfaces.dto.DashboardDailyResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock DashboardMapper dashboardMapper;
    @Mock BudgetMapper budgetMapper;

    private DashboardService dashboardService() {
        return new DashboardService(dashboardMapper, budgetMapper);
    }

    private static DailyAmountRow row(String date, int type, String amount) {
        DailyAmountRow row = new DailyAmountRow();
        setField(row, "txDate", date);
        setField(row, "type", type);
        setField(row, "amount", new BigDecimal(amount));
        return row;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void 거래가_없는_날은_0으로_채워지고_없는_날짜만큼_길이가_맞다() {
        given(dashboardMapper.sumDailyAmounts(1L, "2026-06")).willReturn(List.of());
        given(budgetMapper.findByUserIdAndYearMonth(1L, "2026-06")).willReturn(null);

        DashboardDailyResponse response = dashboardService().getDailyBalances(1L, "202606");

        assertThat(response.days()).hasSize(30); // 2026-06 has 30 days
        assertThat(response.days().get(0).expense()).isEqualByComparingTo("0");
        assertThat(response.days().get(0).cumulativeExpense()).isEqualByComparingTo("0");
        assertThat(response.budgetAmount()).isNull();
    }

    @Test
    void 지출과_수입이_날짜순으로_누적된다() {
        given(dashboardMapper.sumDailyAmounts(1L, "2026-06")).willReturn(List.of(
                row("2026-06-01", 0, "100000"),
                row("2026-06-03", 0, "50000"),
                row("2026-06-03", 1, "3000000")
        ));
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal("2000000"));
        given(budgetMapper.findByUserIdAndYearMonth(1L, "2026-06")).willReturn(budget);

        DashboardDailyResponse response = dashboardService().getDailyBalances(1L, "202606");

        assertThat(response.budgetAmount()).isEqualByComparingTo("2000000");
        assertThat(response.days().get(0).cumulativeExpense()).isEqualByComparingTo("100000");
        assertThat(response.days().get(1).cumulativeExpense()).isEqualByComparingTo("100000"); // day 2, unchanged
        assertThat(response.days().get(2).cumulativeExpense()).isEqualByComparingTo("150000"); // day 3
        assertThat(response.days().get(2).cumulativeBalance()).isEqualByComparingTo("2850000"); // 3,000,000 - 150,000
    }
}
