package com.example.sales_summery.dashboard.dto;

import com.example.sales_summery.dashboard.repository.ProfitSummaryProjection;
import java.math.BigDecimal;

public record ProfitSummaryResponse(BigDecimal totalIncome, BigDecimal totalExpense,
                                    BigDecimal netProfit) {
    public static ProfitSummaryResponse from(ProfitSummaryProjection projection) {
        BigDecimal income = zeroIfNull(projection == null ? null : projection.getTotalIncome());
        BigDecimal expense = zeroIfNull(projection == null ? null : projection.getTotalExpense());
        return new ProfitSummaryResponse(income, expense, income.subtract(expense));
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
