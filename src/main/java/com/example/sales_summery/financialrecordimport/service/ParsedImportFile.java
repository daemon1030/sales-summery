package com.example.sales_summery.financialrecordimport.service;

import java.util.List;
import java.util.Map;

record ParsedImportFile(
        String fileName,
        String fileHash,
        List<ParsedImportRow> rows,
        Map<String, String> displayHeaders,
        List<String> errors
) {
    int recordCount() {
        return rows.stream().mapToInt(row -> row.amounts().size()).sum();
    }

    int noteCount() {
        return (int) rows.stream().filter(row -> row.note() != null && !row.note().isBlank()).count();
    }
}
