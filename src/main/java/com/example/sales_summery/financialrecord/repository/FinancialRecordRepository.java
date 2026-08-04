package com.example.sales_summery.financialrecord.repository;

import com.example.sales_summery.financialrecord.domain.FinancialRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.dashboard.repository.CategoryTotalProjection;
import com.example.sales_summery.dashboard.repository.ProfitSummaryProjection;

public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    // 현재 사용자가 소유한 기록만 조회
    Optional<FinancialRecord> findByRecordIdAndCategoryUserUserId(Long recordId, Long userId);

    // 사용자 기록을 날짜 범위와 최신 날짜순으로 조회
    List<FinancialRecord> findAllByCategoryUserUserIdAndRecordDateBetweenOrderByRecordDateDesc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    // 사용자 소유 범위 안에서 기간·항목·수입지출 조건을 조합해 검색한다.
    @Query("""
            select r from FinancialRecord r join r.category c
            where c.user.userId = :userId
              and r.recordDate between :startDate and :endDate
              and (:categoryId is null or c.categoryId = :categoryId)
              and (:transactionType is null or c.transactionType = :transactionType)
            """)
    Page<FinancialRecord> search(@Param("userId") Long userId,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("categoryId") Long categoryId,
                                 @Param("transactionType") TransactionType transactionType,
                                 Pageable pageable);

    // DB에서 수입과 지출을 조건부 합산해 대시보드 전송량을 줄인다.
    @Query("""
            select coalesce(sum(case when c.transactionType = com.example.sales_summery.category.domain.TransactionType.INCOME
                                then r.amount else 0 end), 0) as totalIncome,
                   coalesce(sum(case when c.transactionType = com.example.sales_summery.category.domain.TransactionType.EXPENSE
                                then r.amount else 0 end), 0) as totalExpense
            from FinancialRecord r join r.category c
            where c.user.userId = :userId and r.recordDate between :startDate and :endDate
            """)
    ProfitSummaryProjection summarize(@Param("userId") Long userId,
                                      @Param("startDate") LocalDate startDate,
                                      @Param("endDate") LocalDate endDate);

    @Query("""
            select c.categoryId as categoryId, c.categoryName as categoryName,
                   c.transactionType as transactionType, sum(r.amount) as totalAmount
            from FinancialRecord r join r.category c
            where c.user.userId = :userId and r.recordDate between :startDate and :endDate
            group by c.categoryId, c.categoryName, c.transactionType
            order by c.categoryName asc
            """)
    List<CategoryTotalProjection> summarizeByCategory(@Param("userId") Long userId,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    Page<FinancialRecord> findAllByCategoryUserUserIdOrderByRecordDateDescCreatedAtDesc(
            Long userId, Pageable pageable);
}
