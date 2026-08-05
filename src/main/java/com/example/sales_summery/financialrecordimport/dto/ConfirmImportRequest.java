package com.example.sales_summery.financialrecordimport.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ConfirmImportRequest(
        @NotNull List<@Valid ImportColumnMappingRequest> mappings
) {
}
