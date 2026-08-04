package com.example.sales_summery.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.service.CategoryService;
import com.example.sales_summery.dashboard.service.DashboardService;
import com.example.sales_summery.financialrecord.dto.CreateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.service.FinancialRecordService;
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

    private void create(Long userId, Long categoryId, LocalDate date, String amount) {
        recordService.create(userId, new CreateFinancialRecordRequest(
                categoryId, date, new BigDecimal(amount), null));
    }

    private SignupResponse signup(String loginId) {
        return signupService.signup(new SignupRequest(loginId, "password123", "가게"));
    }
}
