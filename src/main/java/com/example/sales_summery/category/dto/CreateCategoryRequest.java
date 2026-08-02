package com.example.sales_summery.category.dto;

import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.Frequency;
import com.example.sales_summery.category.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "항목명은 필수입니다.")
        @Size(max = 100, message = "항목명은 100자 이하여야 합니다.") String categoryName,
        @NotNull(message = "거래 유형은 필수입니다.") TransactionType transactionType,
        @NotNull(message = "비용 유형은 필수입니다.") CostType costType,
        @NotNull(message = "발생 주기는 필수입니다.") Frequency frequency
) {
}
