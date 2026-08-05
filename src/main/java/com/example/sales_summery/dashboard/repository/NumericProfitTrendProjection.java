package com.example.sales_summery.dashboard.repository;

import java.math.BigDecimal;

public interface NumericProfitTrendProjection {
    Integer getPeriod();
    BigDecimal getTotalIncome();
    BigDecimal getTotalExpense();
}
