package com.example.sales_summery.financialrecordimport.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

record ParsedImportRow(
        int sourceRowNumber,
        LocalDate date,
        Map<String, BigDecimal> amounts,
        String note
) {
}
