package com.example.sales_summery.dashboard.dto;

import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.dashboard.repository.CategoryTotalProjection;
import java.math.BigDecimal;

public record CategoryTotalResponse(Long categoryId, String categoryName,
                                    TransactionType transactionType, BigDecimal totalAmount) {
    public static CategoryTotalResponse from(CategoryTotalProjection projection) {
        return new CategoryTotalResponse(projection.getCategoryId(), projection.getCategoryName(),
                projection.getTransactionType(), projection.getTotalAmount());
    }
}
