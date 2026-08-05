package com.example.sales_summery.financialrecordimport.dto;

import java.util.List;

public record ImportFileAnalysisResponse(
        String fileName,
        ImportFileStatus status,
        int rowCount,
        int recordCount,
        int noteCount,
        List<String> errors
) {
}
