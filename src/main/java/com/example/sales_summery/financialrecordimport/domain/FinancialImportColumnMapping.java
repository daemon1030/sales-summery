package com.example.sales_summery.financialrecordimport.domain;

import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.global.common.BaseEntity;
import com.example.sales_summery.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "financial_import_column_mappings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_financial_import_mappings_user_header",
                columnNames = {"user_id", "normalized_header"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialImportColumnMapping extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_financial_import_mappings_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_financial_import_mappings_category"))
    private FinancialCategory category;

    @Column(name = "normalized_header", nullable = false, length = 255)
    private String normalizedHeader;

    private FinancialImportColumnMapping(User user, FinancialCategory category, String normalizedHeader) {
        this.user = user;
        this.category = category;
        this.normalizedHeader = normalizedHeader;
    }

    public static FinancialImportColumnMapping create(User user, FinancialCategory category, String normalizedHeader) {
        return new FinancialImportColumnMapping(user, category, normalizedHeader);
    }

    public void changeCategory(FinancialCategory category) {
        this.category = category;
    }
}
