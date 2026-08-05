package com.example.sales_summery.financialrecordimport.dto;

import java.util.List;

public record ImportAnalysisResponse(
        String importToken,
        List<ImportFileAnalysisResponse> files,
        List<ImportColumnAnalysisResponse> columns,
        boolean requiresMapping,
        int readyFileCount,
        int duplicateFileCount,
        int invalidFileCount
) {
}
