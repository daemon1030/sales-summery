package com.example.sales_summery.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.Frequency;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.dto.ChangeCategoryNameRequest;
import com.example.sales_summery.category.dto.CreateCategoryRequest;
import com.example.sales_summery.category.service.CategoryService;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.CategoryNotFoundException;
import com.example.sales_summery.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoryServiceIntegrationTest {
    @Autowired SignupService signupService;
    @Autowired CategoryService categoryService;

    @Test
    void categoryCanBeCreatedRenamedAndActivated() {
        SignupResponse user = signup("category1");
        var created = categoryService.create(user.userId(), new CreateCategoryRequest(
                "배달 매출", TransactionType.INCOME, CostType.NONE, Frequency.DAILY));

        var renamed = categoryService.changeName(user.userId(), created.categoryId(),
                new ChangeCategoryNameRequest("온라인 매출"));
        assertThat(renamed.transactionType()).isEqualTo(TransactionType.INCOME);
        assertThat(renamed.costType()).isEqualTo(CostType.NONE);
        assertThat(categoryService.deactivate(user.userId(), created.categoryId()).active()).isFalse();
        assertThat(categoryService.getCategories(user.userId(), true))
                .noneMatch(category -> category.categoryId().equals(created.categoryId()));
        assertThat(categoryService.activate(user.userId(), created.categoryId()).active()).isTrue();
    }

    @Test
    void duplicateNameWithinSameUserIsRejected() {
        SignupResponse user = signup("category2");

        assertThatThrownBy(() -> categoryService.create(user.userId(), new CreateCategoryRequest(
                "월세", TransactionType.EXPENSE, CostType.FIXED, Frequency.MONTHLY)))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_CATEGORY_NAME));
    }

    @Test
    void categoryOwnedByAnotherUserLooksNotFound() {
        SignupResponse owner = signup("category3");
        SignupResponse other = signup("category4");
        Long categoryId = categoryService.getCategories(owner.userId(), false).getFirst().categoryId();

        assertThatThrownBy(() -> categoryService.getCategory(other.userId(), categoryId))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThatThrownBy(() -> categoryService.deactivate(other.userId(), categoryId))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void invalidIncomeCostTypeIsRejectedByDomain() {
        SignupResponse user = signup("category5");
        assertThatThrownBy(() -> categoryService.create(user.userId(), new CreateCategoryRequest(
                "잘못된 수입", TransactionType.INCOME, CostType.FIXED, Frequency.DAILY)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private SignupResponse signup(String loginId) {
        return signupService.signup(new SignupRequest(loginId, "password123", "가게"));
    }
}
