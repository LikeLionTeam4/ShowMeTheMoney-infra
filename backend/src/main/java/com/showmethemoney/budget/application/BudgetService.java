package com.showmethemoney.budget.application;

import com.showmethemoney.budget.domain.Budget;
import com.showmethemoney.budget.infrastructure.BudgetMapper;
import com.showmethemoney.budget.interfaces.dto.BudgetResponse;
import com.showmethemoney.budget.interfaces.dto.CreateBudgetRequest;
import com.showmethemoney.budget.interfaces.dto.UpdateBudgetRequest;
import com.showmethemoney.common.BusinessException;
import com.showmethemoney.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private final BudgetMapper budgetMapper;

    public BudgetService(BudgetMapper budgetMapper) {
        this.budgetMapper = budgetMapper;
    }

    @Transactional
    public void create(Long userId, CreateBudgetRequest request) {
        String yearMonth = toDbYearMonth(request.yearMonth());
        if (budgetMapper.findByUserIdAndYearMonth(userId, yearMonth) != null) {
            throw new BusinessException(ErrorCode.BUDGET_ALREADY_EXISTS);
        }
        Budget budget = new Budget();
        budget.setUuidUser(userId);
        budget.setYearMonth(yearMonth);
        budget.setAmount(request.amount());
        budgetMapper.insert(budget);
    }

    @Transactional(readOnly = true)
    public BudgetResponse get(Long userId, String yearMonth) {
        Budget budget = budgetMapper.findByUserIdAndYearMonth(userId, toDbYearMonth(yearMonth));
        if (budget == null) throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
        return new BudgetResponse(budget.getUuid(), budget.getYearMonth(), budget.getAmount());
    }

    @Transactional
    public void update(Long userId, Long id, UpdateBudgetRequest request) {
        Budget budget = budgetMapper.findById(id);
        if (budget == null) throw new BusinessException(ErrorCode.BUDGET_NOT_FOUND);
        if (!budget.getUuidUser().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        budgetMapper.update(id, request.amount());
    }

    // "202606" → "2026-06"
    private String toDbYearMonth(String ym) {
        if (ym == null || ym.length() != 6) throw new BusinessException(ErrorCode.INVALID_INPUT);
        return ym.substring(0, 4) + "-" + ym.substring(4);
    }
}
