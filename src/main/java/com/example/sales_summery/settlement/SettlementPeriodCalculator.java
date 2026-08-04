package com.example.sales_summery.settlement;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

// DB나 HTTP 없이 정산 시작일과 기준 날짜만으로 기간을 계산하는 순수 컴포넌트다.
@Component
public class SettlementPeriodCalculator {
    private final Clock clock;

    public SettlementPeriodCalculator(Clock clock) {
        this.clock = clock;
    }

    public SettlementPeriod current(int settlementStartDay) {
        return calculate(settlementStartDay, LocalDate.now(clock));
    }

    public SettlementPeriod calculate(int settlementStartDay, LocalDate referenceDate) {
        if (settlementStartDay < 1 || settlementStartDay > 28) {
            throw new IllegalArgumentException("settlementStartDay must be between 1 and 28");
        }
        LocalDate thisMonthStart = referenceDate.withDayOfMonth(settlementStartDay);
        LocalDate start = referenceDate.getDayOfMonth() >= settlementStartDay
                ? thisMonthStart : thisMonthStart.minusMonths(1);
        return new SettlementPeriod(start, start.plusMonths(1).minusDays(1));
    }
}
