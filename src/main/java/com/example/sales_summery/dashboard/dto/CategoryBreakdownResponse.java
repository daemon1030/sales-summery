package com.example.sales_summery.dashboard.dto;

import com.example.sales_summery.category.domain.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CategoryBreakdownResponse(int year, int month, TransactionType transactionType,
                                        LocalDate startDate, LocalDate endDate,
                                        BigDecimal totalAmount,
                                        List<CategoryBreakdownItemResponse> items) {
}
