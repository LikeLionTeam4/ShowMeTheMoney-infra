package com.showmethemoney.transaction.infrastructure;

import com.showmethemoney.transaction.domain.Transaction;
import com.showmethemoney.transaction.application.TransactionListRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TransactionMapper {

    void insert(Transaction transaction);

    boolean existsByRecurringItemAndDate(@Param("uuidRecurringItem") Long uuidRecurringItem, @Param("date") LocalDate date);

    List<Transaction> findAll(@Param("userId") Long userId, @Param("request") TransactionListRequest request, @Param("offset") int offset);

    long countAll(@Param("userId") Long userId, @Param("request") TransactionListRequest request);

    BigDecimal sumAmountByType(@Param("userId") Long userId, @Param("request") TransactionListRequest request, @Param("type") int type);

    Transaction findById(Long id);

    void update(Transaction transaction);

    void softDelete(Long id);
}
