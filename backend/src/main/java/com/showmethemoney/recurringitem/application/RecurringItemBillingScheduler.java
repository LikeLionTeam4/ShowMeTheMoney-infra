package com.showmethemoney.recurringitem.application;

import com.showmethemoney.recurringitem.domain.RecurringItem;
import com.showmethemoney.recurringitem.infrastructure.RecurringItemMapper;
import com.showmethemoney.transaction.domain.Transaction;
import com.showmethemoney.transaction.infrastructure.TransactionMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 활성 고정 수입/지출을 결제일에 실제 transactions 레코드로 반영한다 (이슈 #13).
 * 매일 새벽 실행되며, 서버가 특정 날짜에 내려가 있었던 경우를 대비해
 * 기동 시점에도 한 번 실행해 이번 달 놓친 결제일을 소급 생성한다.
 */
@Component
public class RecurringItemBillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringItemBillingScheduler.class);

    private final RecurringItemMapper recurringItemMapper;
    private final TransactionMapper transactionMapper;

    public RecurringItemBillingScheduler(RecurringItemMapper recurringItemMapper, TransactionMapper transactionMapper) {
        this.recurringItemMapper = recurringItemMapper;
        this.transactionMapper = transactionMapper;
    }

    @PostConstruct
    public void onStartup() {
        generateDueTransactions(LocalDate.now());
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void onSchedule() {
        generateDueTransactions(LocalDate.now());
    }

    @Transactional
    public void generateDueTransactions(LocalDate today) {
        List<RecurringItem> items = recurringItemMapper.findAllActive();
        for (RecurringItem item : items) {
            LocalDate billingDate = billingDateForMonth(item.getBillingDay(), today);
            if (billingDate.isAfter(today)) continue;
            if (transactionMapper.existsByRecurringItemAndDate(item.getUuid(), billingDate)) continue;

            Transaction transaction = new Transaction();
            transaction.setUuidUser(item.getUuidUser());
            transaction.setUuidCategory(item.getUuidCategory());
            transaction.setUuidRecurringItem(item.getUuid());
            transaction.setType(item.getType());
            transaction.setAmount(item.getAmount());
            transaction.setMemo(item.getName());
            transaction.setTransactionDate(billingDate);
            transactionMapper.insert(transaction);

            log.info("Generated recurring transaction: recurringItem={}, user={}, date={}",
                    item.getUuid(), item.getUuidUser(), billingDate);
        }
    }

    /** billingDay가 해당 월의 마지막 날보다 크면(예: 31일, 2월) 그 달의 마지막 날로 맞춘다. */
    private LocalDate billingDateForMonth(int billingDay, LocalDate month) {
        int day = Math.min(billingDay, month.lengthOfMonth());
        return month.withDayOfMonth(day);
    }
}
