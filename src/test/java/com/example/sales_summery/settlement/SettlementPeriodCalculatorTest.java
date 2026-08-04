package com.example.sales_summery.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SettlementPeriodCalculatorTest {
    private final SettlementPeriodCalculator calculator = new SettlementPeriodCalculator(
            Clock.system(ZoneId.of("Asia/Seoul")));

    @Test
    void dayOnOrAfterStartUsesCurrentMonth() {
        assertThat(calculator.calculate(8, LocalDate.of(2026, 7, 30)))
                .isEqualTo(new SettlementPeriod(LocalDate.of(2026, 7, 8), LocalDate.of(2026, 8, 7)));
    }

    @Test
    void dayBeforeStartUsesPreviousMonth() {
        assertThat(calculator.calculate(8, LocalDate.of(2026, 7, 5)))
                .isEqualTo(new SettlementPeriod(LocalDate.of(2026, 6, 8), LocalDate.of(2026, 7, 7)));
    }

    @Test
    void yearBoundaryAndStartDayOneAreHandled() {
        assertThat(calculator.calculate(1, LocalDate.of(2026, 1, 1)))
                .isEqualTo(new SettlementPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)));
        assertThat(calculator.calculate(28, LocalDate.of(2026, 1, 3)))
                .isEqualTo(new SettlementPeriod(LocalDate.of(2025, 12, 28), LocalDate.of(2026, 1, 27)));
    }

    @Test
    void februaryAndLeapYearAreHandledByLocalDate() {
        assertThat(calculator.calculate(28, LocalDate.of(2024, 2, 28)))
                .isEqualTo(new SettlementPeriod(LocalDate.of(2024, 2, 28), LocalDate.of(2024, 3, 27)));
        assertThat(calculator.calculate(1, LocalDate.of(2024, 2, 29)).endDate())
                .isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void invalidStartDayIsRejected() {
        assertThatThrownBy(() -> calculator.calculate(29, LocalDate.of(2026, 8, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
