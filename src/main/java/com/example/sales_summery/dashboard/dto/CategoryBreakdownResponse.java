package com.example.sales_summery.dashboard.dto;

import com.example.sales_summery.category.domain.TransactionType;
import java.math.BigDecimal;
import java.util.List;

public record CategoryBreakdownResponse(int year, int month, TransactionType transactionType,
                                        BigDecimal totalAmount,
                                        List<CategoryBreakdownItemResponse> items) {
}
