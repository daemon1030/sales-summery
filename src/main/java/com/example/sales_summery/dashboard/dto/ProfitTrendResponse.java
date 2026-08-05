package com.example.sales_summery.dashboard.dto;

import java.math.BigDecimal;

public record ProfitTrendResponse(String period, BigDecimal totalIncome,
                                  BigDecimal totalExpense, BigDecimal netProfit) {
    public static ProfitTrendResponse of(String period, BigDecimal income, BigDecimal expense) {
        BigDecimal safeIncome = income == null ? BigDecimal.ZERO : income;
        BigDecimal safeExpense = expense == null ? BigDecimal.ZERO : expense;
        return new ProfitTrendResponse(period, safeIncome, safeExpense,
                safeIncome.subtract(safeExpense));
    }

    public static ProfitTrendResponse empty(String period) {
        return of(period, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
