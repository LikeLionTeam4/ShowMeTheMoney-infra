package com.showmethemoney.dashboard.interfaces;

import com.showmethemoney.common.ApiResponse;
import com.showmethemoney.dashboard.application.DashboardService;
import com.showmethemoney.dashboard.interfaces.dto.CategoryExpenseResponse;
import com.showmethemoney.dashboard.interfaces.dto.DashboardDailyResponse;
import com.showmethemoney.dashboard.interfaces.dto.DashboardSummaryResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary(
            @AuthenticationPrincipal Long userId,
            @RequestParam("yearMonth") String yearMonth) {
        return ApiResponse.ok(dashboardService.getSummary(userId, yearMonth));
    }

    @GetMapping("/categories")
    public ApiResponse<List<CategoryExpenseResponse>> getCategoryExpenses(
            @AuthenticationPrincipal Long userId,
            @RequestParam("yearMonth") String yearMonth,
            @RequestParam(name = "type", required = false) Integer type) {
        return ApiResponse.ok(dashboardService.getCategoryExpenses(userId, yearMonth, type));
    }

    @GetMapping("/daily")
    public ApiResponse<DashboardDailyResponse> getDailyBalances(
            @AuthenticationPrincipal Long userId,
            @RequestParam("yearMonth") String yearMonth) {
        return ApiResponse.ok(dashboardService.getDailyBalances(userId, yearMonth));
    }
}
