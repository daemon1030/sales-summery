package com.example.sales_summery.financialrecordimport.dto;

public record ImportResultResponse(
        int importedFileCount,
        int duplicateFileCount,
        int createdCategoryCount,
        int createdRecordCount,
        int createdNoteCount,
        int skippedNoteCount
) {
}
