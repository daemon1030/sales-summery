package com.example.sales_summery.dashboard.service;

import com.example.sales_summery.dashboard.dto.CategoryTotalResponse;
import com.example.sales_summery.dashboard.dto.PeriodSummaryResponse;
import com.example.sales_summery.dashboard.dto.ProfitSummaryResponse;
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
    public List<FinancialRecordResponse> recent(Long userId) {
        // Pageable로 DB 조회 자체를 5건으로 제한한다.
        return recordRepository.findAllByCategoryUserUserIdOrderByRecordDateDescCreatedAtDesc(
                userId, PageRequest.of(0, 5)).stream().map(FinancialRecordResponse::from).toList();
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
    }
}
