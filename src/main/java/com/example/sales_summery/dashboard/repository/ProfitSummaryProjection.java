package com.example.sales_summery.dashboard.repository;

import java.math.BigDecimal;

public interface ProfitSummaryProjection {
    BigDecimal getTotalIncome();
    BigDecimal getTotalExpense();
}
