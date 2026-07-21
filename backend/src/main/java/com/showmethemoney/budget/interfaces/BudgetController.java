package com.showmethemoney.budget.interfaces;

import com.showmethemoney.budget.application.BudgetService;
import com.showmethemoney.budget.interfaces.dto.BudgetResponse;
import com.showmethemoney.budget.interfaces.dto.CreateBudgetRequest;
import com.showmethemoney.budget.interfaces.dto.UpdateBudgetRequest;
import com.showmethemoney.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody CreateBudgetRequest request) {
        budgetService.create(userId, request);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<BudgetResponse> get(@AuthenticationPrincipal Long userId,
                                           @RequestParam("yearMonth") String yearMonth) {
        return ApiResponse.ok(budgetService.get(userId, yearMonth));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@AuthenticationPrincipal Long userId,
                                    @PathVariable("id") Long id,
                                    @Valid @RequestBody UpdateBudgetRequest request) {
        budgetService.update(userId, id, request);
        return ApiResponse.ok();
    }
}
