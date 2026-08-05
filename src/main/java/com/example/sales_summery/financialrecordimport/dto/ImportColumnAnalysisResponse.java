package com.example.sales_summery.financialrecordimport.dto;

import java.math.BigDecimal;

public record ImportColumnAnalysisResponse(
        String header,
        ImportColumnStatus status,
        Long categoryId,
        String categoryName,
        int valueCount,
        BigDecimal totalAmount
) {
}
