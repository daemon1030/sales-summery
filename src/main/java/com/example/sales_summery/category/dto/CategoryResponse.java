package com.example.sales_summery.category.dto;

import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.category.domain.Frequency;
import com.example.sales_summery.category.domain.TransactionType;

public record CategoryResponse(Long categoryId, String categoryName, TransactionType transactionType,
                               CostType costType, Frequency frequency, boolean active) {
    public static CategoryResponse from(FinancialCategory category) {
        return new CategoryResponse(category.getCategoryId(), category.getCategoryName(),
                category.getTransactionType(), category.getCostType(), category.getFrequency(),
                category.isActive());
    }
}
