package com.example.sales_summery.auth.service;

import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.category.domain.Frequency;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.repository.FinancialCategoryRepository;
import com.example.sales_summery.user.domain.User;
import java.util.List;
import org.springframework.stereotype.Component;

// 회원가입 시 모든 사용자에게 필요한 기본 수입·지출 항목을 생성한다.
@Component
public class DefaultCategoryCreator {

    private final FinancialCategoryRepository categoryRepository;

    public DefaultCategoryCreator(FinancialCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void createFor(User user) {
        categoryRepository.saveAll(List.of(
                category(user, "카드 매출", TransactionType.INCOME, CostType.NONE, Frequency.DAILY),
                category(user, "현금 매출", TransactionType.INCOME, CostType.NONE, Frequency.DAILY),
                category(user, "재료비", TransactionType.EXPENSE, CostType.VARIABLE, Frequency.IRREGULAR),
                category(user, "인건비", TransactionType.EXPENSE, CostType.FIXED, Frequency.MONTHLY),
                category(user, "월세", TransactionType.EXPENSE, CostType.FIXED, Frequency.MONTHLY)
        ));
    }

    private FinancialCategory category(User user, String name, TransactionType transactionType,
                                       CostType costType, Frequency frequency) {
        return FinancialCategory.create(user, name, transactionType, costType, frequency);
    }
}
