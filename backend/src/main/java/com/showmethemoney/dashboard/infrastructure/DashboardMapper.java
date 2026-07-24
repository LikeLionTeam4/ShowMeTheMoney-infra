package com.showmethemoney.dashboard.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface DashboardMapper {

    BigDecimal sumAmountByType(@Param("userId") Long userId, @Param("yearMonth") String yearMonth, @Param("type") int type);

    List<CategoryAmountRow> sumByCategory(@Param("userId") Long userId, @Param("yearMonth") String yearMonth, @Param("type") int type);

    List<DailyAmountRow> sumDailyAmounts(@Param("userId") Long userId, @Param("yearMonth") String yearMonth);
}
