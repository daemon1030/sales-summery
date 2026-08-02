package com.example.sales_summery.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SettlementStartDayRequest(
        @Min(value = 1, message = "정산 시작일은 1일 이상이어야 합니다.")
        @Max(value = 28, message = "정산 시작일은 28일 이하여야 합니다.") int settlementStartDay
) {
}
