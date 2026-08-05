package com.example.sales_summery.dashboard.dto;

import java.math.BigDecimal;

public record CategoryBreakdownItemResponse(Long categoryId, String categoryName,
                                            BigDecimal amount, BigDecimal percentage) {
}
