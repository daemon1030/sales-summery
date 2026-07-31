package com.example.sales_summery.category.domain;

import com.example.sales_summery.financialrecord.domain.FinancialRecord;
import com.example.sales_summery.global.common.BaseEntity;
import com.example.sales_summery.user.domain.User;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
// 사용자별 수입·지출 분류 항목
@Table(
        name = "financial_categories",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_financial_categories_user_name",
                columnNames = {"user_id", "category_name"}
        ),
        indexes = @Index(
                name = "idx_financial_categories_user_active",
                columnList = "user_id, is_active"
        ),
        // 수입·지출 유형과 비용 유형의 올바른 조합만 저장
        check = @CheckConstraint(
                name = "chk_financial_categories_type_cost",
                constraint = "(transaction_type = 'INCOME' AND cost_type = 'NONE') "
                        + "OR (transaction_type = 'EXPENSE' AND cost_type IN ('FIXED', 'VARIABLE'))"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialCategory extends BaseEntity {

    // 데이터베이스가 생성하는 항목 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    // 이 항목을 소유한 사용자
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_financial_categories_user")
    )
    private User user;

    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    // 수입 또는 지출에 따라 허용되는 비용 유형이 달라짐
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false, length = 20)
    private CostType costType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frequency frequency;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    // 연결된 기록 목록은 외부에서 직접 변경하지 않음
    @OneToMany(mappedBy = "category")
    @Getter(AccessLevel.NONE)
    private final List<FinancialRecord> financialRecords = new ArrayList<>();

    private FinancialCategory(
            User user,
            String categoryName,
            TransactionType transactionType,
            CostType costType,
            Frequency frequency
    ) {
        validateCostType(transactionType, costType);
        this.user = user;
        this.categoryName = categoryName;
        this.transactionType = transactionType;
        this.costType = costType;
        this.frequency = frequency;
        this.active = true;
    }

    // 유효성 검사를 거치는 생성 경로
    public static FinancialCategory create(
            User user,
            String categoryName,
            TransactionType transactionType,
            CostType costType,
            Frequency frequency
    ) {
        return new FinancialCategory(user, categoryName, transactionType, costType, frequency);
    }

    public void changeName(String categoryName) {
        this.categoryName = categoryName;
    }

    public void activate() {
        this.active = true;
    }

    // 기록이 남은 항목도 삭제 대신 비활성화 가능
    public void deactivate() {
        this.active = false;
    }

    // 목록 자체를 수정할 수 없는 복사본으로 반환
    public List<FinancialRecord> getFinancialRecords() {
        return List.copyOf(financialRecords);
    }

    // 수입은 NONE, 지출은 FIXED 또는 VARIABLE만 허용
    private static void validateCostType(TransactionType transactionType, CostType costType) {
        boolean incomeHasNoneCost = transactionType == TransactionType.INCOME && costType == CostType.NONE;
        boolean expenseHasExpenseCost = transactionType == TransactionType.EXPENSE
                && (costType == CostType.FIXED || costType == CostType.VARIABLE);
        if (!incomeHasNoneCost && !expenseHasExpenseCost) {
            throw new IllegalArgumentException("Invalid transactionType and costType combination");
        }
    }
}
