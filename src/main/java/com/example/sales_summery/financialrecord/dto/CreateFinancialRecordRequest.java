package com.example.sales_summery.financialrecord.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
public record CreateFinancialRecordRequest(
        @NotNull(message = "항목 ID는 필수입니다.") Long categoryId,
        @NotNull(message = "기록 날짜는 필수입니다.") LocalDate recordDate,
        @NotNull(message = "금액은 필수입니다.")
        @DecimalMin(value = "0.0", inclusive = false, message = "금액은 0보다 커야 합니다.") BigDecimal amount,
        @Size(max = 500, message = "메모는 500자 이하여야 합니다.") String memo) {}
