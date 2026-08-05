package com.example.sales_summery.dashboard.service;

import com.example.sales_summery.dashboard.dto.CategoryTotalResponse;
import com.example.sales_summery.dashboard.dto.CategoryBreakdownItemResponse;
import com.example.sales_summery.dashboard.dto.CategoryBreakdownResponse;
import com.example.sales_summery.dashboard.dto.PeriodSummaryResponse;
import com.example.sales_summery.dashboard.dto.ProfitSummaryResponse;
import com.example.sales_summery.dashboard.dto.ProfitTrendResponse;
import com.example.sales_summery.dashboard.dto.TrendUnit;
import com.example.sales_summery.dashboard.repository.DailyProfitTrendProjection;
import com.example.sales_summery.dashboard.repository.NumericProfitTrendProjection;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.financialrecord.dto.FinancialRecordResponse;
import com.example.sales_summery.financialrecord.repository.FinancialRecordRepository;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.UserNotFoundException;
import com.example.sales_summery.settlement.SettlementPeriod;
import com.example.sales_summery.settlement.SettlementPeriodCalculator;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final FinancialRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final SettlementPeriodCalculator periodCalculator;
    private final Clock clock;

    public DashboardService(FinancialRecordRepository recordRepository, UserRepository userRepository,
                            SettlementPeriodCalculator periodCalculator, Clock clock) {
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.periodCalculator = periodCalculator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfitSummaryResponse today(Long userId) {
        LocalDate today = LocalDate.now(clock);
        return summarize(userId, today, today);
    }

    @Transactional(readOnly = true)
    public PeriodSummaryResponse currentSettlement(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        SettlementPeriod period = periodCalculator.current(user.getSettlementStartDay());
        return PeriodSummaryResponse.of(period, summarize(userId, period.startDate(), period.endDate()));
    }

    @Transactional(readOnly = true)
    public ProfitSummaryResponse summarize(Long userId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        return ProfitSummaryResponse.from(recordRepository.summarize(userId, startDate, endDate));
    }

    @Transactional(readOnly = true)
    public List<CategoryTotalResponse> categoryTotals(Long userId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        return recordRepository.summarizeByCategory(userId, startDate, endDate).stream()
                .map(CategoryTotalResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProfitTrendResponse> profitTrend(Long userId, TrendUnit unit,
            LocalDate startDate, LocalDate endDate, Integer year,
            Integer startYear, Integer endYear) {
        if (unit == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "조회 단위는 필수입니다.");
        }
        return switch (unit) {
            case DAILY -> dailyTrend(userId, startDate, endDate);
            case MONTHLY -> monthlyTrend(userId, year);
            case YEARLY -> yearlyTrend(userId, startYear, endYear);
        };
    }

    @Transactional(readOnly = true)
    public CategoryBreakdownResponse categoryBreakdown(Long userId, int year, int month,
            TransactionType transactionType) {
        if (year < 1 || month < 1 || month > 12 || transactionType == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        YearMonth target = YearMonth.of(year, month);
        var projections = recordRepository.summarizeCategoryBreakdown(userId, transactionType,
                target.atDay(1), target.atEndOfMonth());
        BigDecimal total = projections.stream().map(p -> p.getAmount() == null
                        ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CategoryBreakdownItemResponse> items = projections.stream()
                .map(item -> new CategoryBreakdownItemResponse(item.getCategoryId(),
                        item.getCategoryName(), item.getAmount(), percentage(item.getAmount(), total)))
                .toList();
        return new CategoryBreakdownResponse(year, month, transactionType, total, items);
    }

    @Transactional(readOnly = true)
    public List<FinancialRecordResponse> recent(Long userId) {
        // Pageable로 DB 조회 자체를 5건으로 제한한다.
        return recordRepository.findAllByCategoryUserUserIdOrderByRecordDateDescCreatedAtDesc(
                userId, PageRequest.of(0, 5)).stream().map(FinancialRecordResponse::from).toList();
    }

    private List<ProfitTrendResponse> dailyTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        requireDates(startDate, endDate);
        if (ChronoUnit.DAYS.between(startDate, endDate) > 365) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "일별 조회 기간은 최대 366일입니다.");
        }
        Map<LocalDate, DailyProfitTrendProjection> totals = recordRepository
                .summarizeDailyTrend(userId, startDate, endDate).stream()
                .collect(Collectors.toMap(DailyProfitTrendProjection::getPeriod, Function.identity()));
        return startDate.datesUntil(endDate.plusDays(1)).map(date -> {
            DailyProfitTrendProjection value = totals.get(date);
            return value == null ? ProfitTrendResponse.empty(date.toString())
                    : ProfitTrendResponse.of(date.toString(), value.getTotalIncome(), value.getTotalExpense());
        }).toList();
    }

    private List<ProfitTrendResponse> monthlyTrend(Long userId, Integer year) {
        if (year == null || year < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "조회 연도는 필수입니다.");
        }
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        Map<Integer, NumericProfitTrendProjection> totals = recordRepository
                .summarizeMonthlyTrend(userId, startDate, endDate).stream()
                .collect(Collectors.toMap(NumericProfitTrendProjection::getPeriod, Function.identity()));
        return IntStream.rangeClosed(1, 12).mapToObj(month -> {
            NumericProfitTrendProjection value = totals.get(month);
            String period = YearMonth.of(year, month).toString();
            return value == null ? ProfitTrendResponse.empty(period)
                    : ProfitTrendResponse.of(period, value.getTotalIncome(), value.getTotalExpense());
        }).toList();
    }

    private List<ProfitTrendResponse> yearlyTrend(Long userId, Integer startYear, Integer endYear) {
        if (startYear == null || endYear == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "시작 연도와 종료 연도는 필수입니다.");
        }
        if (startYear > endYear) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
        if (endYear - startYear > 9) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "연별 조회 기간은 최대 10년입니다.");
        }
        Map<Integer, NumericProfitTrendProjection> totals = recordRepository.summarizeYearlyTrend(
                        userId, LocalDate.of(startYear, 1, 1), LocalDate.of(endYear, 12, 31)).stream()
                .collect(Collectors.toMap(NumericProfitTrendProjection::getPeriod, Function.identity()));
        return IntStream.rangeClosed(startYear, endYear).mapToObj(valueYear -> {
            NumericProfitTrendProjection value = totals.get(valueYear);
            return value == null ? ProfitTrendResponse.empty(Integer.toString(valueYear))
                    : ProfitTrendResponse.of(Integer.toString(valueYear),
                            value.getTotalIncome(), value.getTotalExpense());
        }).toList();
    }

    private void requireDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "시작일과 종료일은 필수입니다.");
        }
        validateRange(startDate, endDate);
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (amount == null || total.signum() == 0) return BigDecimal.ZERO.setScale(1);
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP);
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
    }
}
