package com.example.sales_summery.financialrecordimport.dto;

import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.Frequency;
import com.example.sales_summery.category.domain.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ImportColumnMappingRequest(
        @NotBlank String header,
        @NotNull ImportMappingAction action,
        Long categoryId,
        @Size(max = 100) String categoryName,
        TransactionType transactionType,
        CostType costType,
        Frequency frequency
) {
}
