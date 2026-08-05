package com.example.sales_summery.dashboard.repository;

import java.math.BigDecimal;

public interface CategoryBreakdownProjection {
    Long getCategoryId();
    String getCategoryName();
    BigDecimal getAmount();
}
