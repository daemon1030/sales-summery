package com.example.sales_summery.dashboard.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyProfitTrendProjection {
    LocalDate getPeriod();
    BigDecimal getTotalIncome();
    BigDecimal getTotalExpense();
}
