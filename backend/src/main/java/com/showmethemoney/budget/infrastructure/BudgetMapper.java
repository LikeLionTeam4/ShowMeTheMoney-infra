package com.showmethemoney.budget.infrastructure;

import com.showmethemoney.budget.domain.Budget;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BudgetMapper {

    void insert(Budget budget);

    Budget findByUserIdAndYearMonth(Long userId, String yearMonth);

    Budget findById(Long id);

    void update(Long id, java.math.BigDecimal amount);
}
