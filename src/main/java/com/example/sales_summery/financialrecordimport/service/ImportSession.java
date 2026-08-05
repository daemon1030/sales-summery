package com.example.sales_summery.financialrecordimport.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

record ImportSession(
        Long userId,
        Instant expiresAt,
        List<ParsedImportFile> files,
        Map<String, Long> autoCategoryIds,
        Map<String, String> displayHeaders,
        Set<String> unmappedHeaders,
        int duplicateFileCount
) {
}
