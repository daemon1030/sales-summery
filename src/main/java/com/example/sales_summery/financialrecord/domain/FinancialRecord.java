package com.example.sales_summery.financialrecord.domain;

import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.global.common.BaseEntity;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
// 개별 수입·지출 금액 기록
@Table(
        name = "financial_records",
        indexes = @Index(
                name = "idx_financial_records_category_date",
                columnList = "category_id, record_date"
        ),
        // 0원 이하 금액을 데이터베이스에서도 차단
        check = @CheckConstraint(name = "chk_financial_records_amount", constraint = "amount > 0")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialRecord extends BaseEntity {

    // 데이터베이스가 생성하는 기록 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    // 기록의 수입·지출 구분은 연결된 항목에서 결정
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_financial_records_category")
    )
    private FinancialCategory category;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    // 소수 오차 없이 금액을 저장
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 500)
    private String memo;

    private FinancialRecord(FinancialCategory category, LocalDate recordDate, BigDecimal amount, String memo) {
        validateAmount(amount);
        this.category = category;
        this.recordDate = recordDate;
        this.amount = amount;
        this.memo = memo;
    }

    // 금액이 양수인지 확인한 뒤 기록 생성
    public static FinancialRecord create(FinancialCategory category, LocalDate recordDate, BigDecimal amount, String memo) {
        return new FinancialRecord(category, recordDate, amount, memo);
    }

    // 수정 시에도 금액 양수 규칙을 유지
    public void change(FinancialCategory category, LocalDate recordDate, BigDecimal amount, String memo) {
        validateAmount(amount);
        this.category = category;
        this.recordDate = recordDate;
        this.amount = amount;
        this.memo = memo;
    }

    // 0원 이하 금액은 허용하지 않음
    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
