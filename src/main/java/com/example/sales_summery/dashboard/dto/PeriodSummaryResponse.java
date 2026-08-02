package com.example.sales_summery.dashboard.dto;

import com.example.sales_summery.settlement.SettlementPeriod;
import java.time.LocalDate;

public record PeriodSummaryResponse(LocalDate startDate, LocalDate endDate,
                                    ProfitSummaryResponse summary) {
    public static PeriodSummaryResponse of(SettlementPeriod period, ProfitSummaryResponse summary) {
        return new PeriodSummaryResponse(period.startDate(), period.endDate(), summary);
    }
}
