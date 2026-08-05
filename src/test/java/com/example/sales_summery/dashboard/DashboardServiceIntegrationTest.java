package com.example.sales_summery.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.service.CategoryService;
import com.example.sales_summery.dashboard.service.DashboardService;
import com.example.sales_summery.dashboard.dto.TrendUnit;
import com.example.sales_summery.financialrecord.dto.CreateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.service.FinancialRecordService;
import com.example.sales_summery.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceIntegrationTest {
    @Autowired SignupService signupService;
    @Autowired CategoryService categoryService;
    @Autowired FinancialRecordService recordService;
    @Autowired DashboardService dashboardService;
    @Autowired UserRepository userRepository;

    @Test
    void emptySummaryReturnsZerosInsteadOfNull() {
        SignupResponse user = signup("dash_01");
        var summary = dashboardService.summarize(user.userId(),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(summary.totalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.totalExpense()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.netProfit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void summaryCalculatesIncomeExpenseNetProfitAndCategoryTotals() {
        SignupResponse user = signup("dash_02");
        var categories = categoryService.getCategories(user.userId(), true);
        var income = categories.stream().filter(c -> c.transactionType() == TransactionType.INCOME).findFirst().orElseThrow();
        var expense = categories.stream().filter(c -> c.transactionType() == TransactionType.EXPENSE).findFirst().orElseThrow();
        LocalDate date = LocalDate.of(2026, 8, 2);
        create(user.userId(), income.categoryId(), date, "10000");
        create(user.userId(), income.categoryId(), date, "5000");
        create(user.userId(), expense.categoryId(), date, "3000");

        var summary = dashboardService.summarize(user.userId(), date, date);
        assertThat(summary.totalIncome()).isEqualByComparingTo("15000");
        assertThat(summary.totalExpense()).isEqualByComparingTo("3000");
        assertThat(summary.netProfit()).isEqualByComparingTo("12000");
        assertThat(dashboardService.categoryTotals(user.userId(), date, date))
                .extracting("totalAmount").containsExactlyInAnyOrder(new BigDecimal("15000.00"), new BigDecimal("3000.00"));
    }

    @Test
    void recentRecordsAreLimitedToFive() {
        SignupResponse user = signup("dash_03");
        Long categoryId = categoryService.getCategories(user.userId(), true).getFirst().categoryId();
        for (int day = 1; day <= 6; day++) {
            create(user.userId(), categoryId, LocalDate.of(2026, 8, day), Integer.toString(day * 100));
        }
        assertThat(dashboardService.recent(user.userId())).hasSize(5);
        assertThat(dashboardService.recent(user.userId()).getFirst().recordDate())
                .isEqualTo(LocalDate.of(2026, 8, 6));
    }

    @Test
    void dailyAndMonthlyTrendFillPeriodsWithoutRecordsWithZeros() {
        SignupResponse user = signup("dash_04");
        var categories = categoryService.getCategories(user.userId(), true);
        var income = categories.stream().filter(c -> c.transactionType() == TransactionType.INCOME)
                .findFirst().orElseThrow();
        var expense = categories.stream().filter(c -> c.transactionType() == TransactionType.EXPENSE)
                .findFirst().orElseThrow();
        create(user.userId(), income.categoryId(), LocalDate.of(2026, 8, 2), "10000");
        create(user.userId(), expense.categoryId(), LocalDate.of(2026, 8, 2), "3000");

        var daily = dashboardService.profitTrend(user.userId(), TrendUnit.DAILY,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 3), null, null, null);
        assertThat(daily).extracting("period")
                .containsExactly("2026-08-01", "2026-08-02", "2026-08-03");
        assertThat(daily.getFirst().totalIncome()).isEqualByComparingTo("0");
        assertThat(daily.get(1).totalIncome()).isEqualByComparingTo("10000");
        assertThat(daily.get(1).totalExpense()).isEqualByComparingTo("3000");
        assertThat(daily.get(1).netProfit()).isEqualByComparingTo("7000");

        var monthly = dashboardService.profitTrend(user.userId(), TrendUnit.MONTHLY,
                null, null, 2026, null, null);
        assertThat(monthly).hasSize(12);
        assertThat(monthly.getFirst().period()).isEqualTo("2026-01");
        assertThat(monthly.get(7).totalIncome()).isEqualByComparingTo("10000");
        assertThat(monthly.get(8).totalIncome()).isEqualByComparingTo("0");
    }

    @Test
    void yearlyTrendFillsMissingYears() {
        SignupResponse user = signup("dash_05");
        Long incomeId = categoryService.getCategories(user.userId(), true).stream()
                .filter(c -> c.transactionType() == TransactionType.INCOME)
                .findFirst().orElseThrow().categoryId();
        create(user.userId(), incomeId, LocalDate.of(2026, 3, 1), "25000");

        var yearly = dashboardService.profitTrend(user.userId(), TrendUnit.YEARLY,
                null, null, null, 2025, 2027);
        assertThat(yearly).extracting("period").containsExactly("2025", "2026", "2027");
        assertThat(yearly.getFirst().totalIncome()).isEqualByComparingTo("0");
        assertThat(yearly.get(1).totalIncome()).isEqualByComparingTo("25000");
        assertThat(yearly.get(2).totalIncome()).isEqualByComparingTo("0");
    }

    @Test
    void categoryBreakdownIncludesInactiveCategoriesAndCalculatesPercentages() {
        SignupResponse user = signup("dash_06");
        userRepository.findById(user.userId()).orElseThrow().changeSettlementStartDay(8);
        var expenses = categoryService.getCategories(user.userId(), true).stream()
                .filter(c -> c.transactionType() == TransactionType.EXPENSE).toList();
        var larger = expenses.get(0);
        var smaller = expenses.get(1);
        LocalDate date = LocalDate.of(2026, 8, 10);
        create(user.userId(), larger.categoryId(), date, "8000");
        create(user.userId(), smaller.categoryId(), date, "2000");
        create(user.userId(), larger.categoryId(), LocalDate.of(2026, 8, 7), "50000");
        create(user.userId(), larger.categoryId(), LocalDate.of(2026, 9, 7), "800");
        create(user.userId(), smaller.categoryId(), LocalDate.of(2026, 9, 7), "200");
        create(user.userId(), larger.categoryId(), LocalDate.of(2026, 9, 8), "50000");
        categoryService.deactivate(user.userId(), larger.categoryId());

        var breakdown = dashboardService.categoryBreakdown(
                user.userId(), 2026, 8, TransactionType.EXPENSE);

        assertThat(breakdown.totalAmount()).isEqualByComparingTo("11000");
        assertThat(breakdown.startDate()).isEqualTo(LocalDate.of(2026, 8, 8));
        assertThat(breakdown.endDate()).isEqualTo(LocalDate.of(2026, 9, 7));
        assertThat(breakdown.items()).extracting("categoryId")
                .containsExactly(larger.categoryId(), smaller.categoryId());
        assertThat(breakdown.items()).extracting("percentage")
                .containsExactly(new BigDecimal("80.0"), new BigDecimal("20.0"));
    }

    private void create(Long userId, Long categoryId, LocalDate date, String amount) {
        recordService.create(userId, new CreateFinancialRecordRequest(
                categoryId, date, new BigDecimal(amount), null));
    }

    private SignupResponse signup(String loginId) {
        return signupService.signup(new SignupRequest(loginId, "password123", "가게"));
    }
}
