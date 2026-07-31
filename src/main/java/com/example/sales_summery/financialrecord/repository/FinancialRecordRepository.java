package com.example.sales_summery.financialrecord.repository;

import com.example.sales_summery.financialrecord.domain.FinancialRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    // 현재 사용자가 소유한 기록만 조회
    Optional<FinancialRecord> findByRecordIdAndCategoryUserUserId(Long recordId, Long userId);

    // 사용자 기록을 날짜 범위와 최신 날짜순으로 조회
    List<FinancialRecord> findAllByCategoryUserUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
