package com.example.sales_summery.dashboard.controller;

import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.dashboard.dto.CategoryTotalResponse;
import com.example.sales_summery.dashboard.dto.CategoryBreakdownResponse;
import com.example.sales_summery.dashboard.dto.PeriodSummaryResponse;
import com.example.sales_summery.dashboard.dto.ProfitSummaryResponse;
import com.example.sales_summery.dashboard.dto.ProfitTrendResponse;
import com.example.sales_summery.dashboard.dto.TrendUnit;
import com.example.sales_summery.category.domain.TransactionType;
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

    @GetMapping("/profit-trend")
    public ApiResponse<List<ProfitTrendResponse>> profitTrend(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam TrendUnit unit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear) {
        return ApiResponse.success(dashboardService.profitTrend(user.userId(), unit,
                startDate, endDate, year, startYear, endYear));
    }

    @GetMapping("/category-breakdown")
    public ApiResponse<CategoryBreakdownResponse> categoryBreakdown(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam TransactionType transactionType) {
        return ApiResponse.success(dashboardService.categoryBreakdown(
                user.userId(), year, month, transactionType));
    }

    @GetMapping("/recent")
    public ApiResponse<List<FinancialRecordResponse>> recent(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(dashboardService.recent(user.userId()));
    }
}
