package com.example.sales_summery.financialrecord.dto;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.financialrecord.domain.FinancialRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record FinancialRecordResponse(Long recordId, Long categoryId, String categoryName,
        TransactionType transactionType, LocalDate recordDate, BigDecimal amount,
        String memo, LocalDateTime createdAt) {
    public static FinancialRecordResponse from(FinancialRecord record) {
        return new FinancialRecordResponse(record.getRecordId(), record.getCategory().getCategoryId(),
                record.getCategory().getCategoryName(), record.getCategory().getTransactionType(),
                record.getRecordDate(), record.getAmount(), record.getMemo(), record.getCreatedAt());
    }
}
