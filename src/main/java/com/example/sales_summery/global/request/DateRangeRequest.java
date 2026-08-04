package com.example.sales_summery.global.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

// 기록, 특이사항, 대시보드의 기간 조회에서 함께 사용하는 요청 DTO다.
public record DateRangeRequest(
        @NotNull(message = "시작일은 필수입니다.") LocalDate startDate,
        @NotNull(message = "종료일은 필수입니다.") LocalDate endDate
) {
    // 필수값은 @NotNull에 맡기고 두 날짜가 있을 때만 순서를 검사한다.
    @AssertTrue(message = "시작일은 종료일보다 늦을 수 없습니다.")
    public boolean isValidDateRange() {
        return startDate == null || endDate == null || !startDate.isAfter(endDate);
    }
}
