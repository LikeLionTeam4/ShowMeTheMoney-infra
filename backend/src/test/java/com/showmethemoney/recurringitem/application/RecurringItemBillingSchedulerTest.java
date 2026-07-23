package com.showmethemoney.recurringitem.application;

import com.showmethemoney.recurringitem.domain.RecurringItem;
import com.showmethemoney.recurringitem.infrastructure.RecurringItemMapper;
import com.showmethemoney.transaction.domain.Transaction;
import com.showmethemoney.transaction.infrastructure.TransactionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecurringItemBillingSchedulerTest {

    @Mock RecurringItemMapper recurringItemMapper;
    @Mock TransactionMapper transactionMapper;

    private RecurringItem item(Long uuid, Long userId, int billingDay) {
        RecurringItem item = new RecurringItem();
        item.setUuid(uuid);
        item.setUuidUser(userId);
        item.setUuidCategory(10L);
        item.setType(0);
        item.setName("월세");
        item.setAmount(new BigDecimal("650000"));
        item.setBillingDay(billingDay);
        item.setIsActive(true);
        return item;
    }

    @Test
    void 결제일이_도래하면_거래를_생성한다() {
        RecurringItem item = item(1L, 100L, 15);
        given(recurringItemMapper.findAllActive()).willReturn(List.of(item));
        given(transactionMapper.existsByRecurringItemAndDate(1L, LocalDate.of(2026, 7, 15))).willReturn(false);

        RecurringItemBillingScheduler scheduler = new RecurringItemBillingScheduler(recurringItemMapper, transactionMapper);
        scheduler.generateDueTransactions(LocalDate.of(2026, 7, 15));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMapper).insert(captor.capture());
        Transaction tx = captor.getValue();
        assertThat(tx.getUuidUser()).isEqualTo(100L);
        assertThat(tx.getUuidRecurringItem()).isEqualTo(1L);
        assertThat(tx.getAmount()).isEqualByComparingTo("650000");
        assertThat(tx.getTransactionDate()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void 이미_생성된_거래는_중복생성하지_않는다() {
        RecurringItem item = item(1L, 100L, 15);
        given(recurringItemMapper.findAllActive()).willReturn(List.of(item));
        given(transactionMapper.existsByRecurringItemAndDate(1L, LocalDate.of(2026, 7, 15))).willReturn(true);

        RecurringItemBillingScheduler scheduler = new RecurringItemBillingScheduler(recurringItemMapper, transactionMapper);
        scheduler.generateDueTransactions(LocalDate.of(2026, 7, 15));

        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void 결제일이_아직_도래하지_않으면_생성하지_않는다() {
        RecurringItem item = item(1L, 100L, 20);
        given(recurringItemMapper.findAllActive()).willReturn(List.of(item));

        RecurringItemBillingScheduler scheduler = new RecurringItemBillingScheduler(recurringItemMapper, transactionMapper);
        scheduler.generateDueTransactions(LocalDate.of(2026, 7, 15));

        verify(transactionMapper, never()).existsByRecurringItemAndDate(any(), any());
        verify(transactionMapper, never()).insert(any());
    }

    @Test
    void billingDay가_해당월_마지막날보다_크면_말일로_clamp한다() {
        RecurringItem item = item(1L, 100L, 31);
        given(recurringItemMapper.findAllActive()).willReturn(List.of(item));
        given(transactionMapper.existsByRecurringItemAndDate(eq(1L), eq(LocalDate.of(2026, 2, 28)))).willReturn(false);

        RecurringItemBillingScheduler scheduler = new RecurringItemBillingScheduler(recurringItemMapper, transactionMapper);
        scheduler.generateDueTransactions(LocalDate.of(2026, 2, 28));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionMapper).insert(captor.capture());
        assertThat(captor.getValue().getTransactionDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
