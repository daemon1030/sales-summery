package com.example.sales_summery.dashboard.repository;

import com.example.sales_summery.category.domain.TransactionType;
import java.math.BigDecimal;

public interface CategoryTotalProjection {
    Long getCategoryId();
    String getCategoryName();
    TransactionType getTransactionType();
    BigDecimal getTotalAmount();
}
