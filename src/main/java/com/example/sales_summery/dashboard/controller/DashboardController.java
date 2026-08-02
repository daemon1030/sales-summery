package com.example.sales_summery.dashboard.controller;

import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.dashboard.dto.CategoryTotalResponse;
import com.example.sales_summery.dashboard.dto.PeriodSummaryResponse;
import com.example.sales_summery.dashboard.dto.ProfitSummaryResponse;
import com.example.sales_summery.dashboard.service.DashboardService;
import com.example.sales_summery.financialrecord.dto.FinancialRecordResponse;
import com.example.sales_summery.global.response.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/today")
    public ApiResponse<ProfitSummaryResponse> today(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(dashboardService.today(user.userId()));
    }

    @GetMapping("/current-settlement")
    public ApiResponse<PeriodSummaryResponse> currentSettlement(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(dashboardService.currentSettlement(user.userId()));
    }

    @GetMapping("/summary")
    public ApiResponse<ProfitSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(dashboardService.summarize(user.userId(), startDate, endDate));
    }

    @GetMapping("/category-totals")
    public ApiResponse<List<CategoryTotalResponse>> categoryTotals(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(dashboardService.categoryTotals(user.userId(), startDate, endDate));
    }

    @GetMapping("/recent")
    public ApiResponse<List<FinancialRecordResponse>> recent(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(dashboardService.recent(user.userId()));
    }
}
