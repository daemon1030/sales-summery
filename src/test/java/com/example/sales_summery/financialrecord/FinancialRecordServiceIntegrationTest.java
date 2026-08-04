package com.example.sales_summery.financialrecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.service.CategoryService;
import com.example.sales_summery.financialrecord.dto.CreateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.dto.UpdateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.service.FinancialRecordService;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.FinancialRecordNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialRecordServiceIntegrationTest {
    @Autowired SignupService signupService;
    @Autowired CategoryService categoryService;
    @Autowired FinancialRecordService recordService;

    @Test
    void recordCanBeCreatedUpdatedMovedAndDeleted() {
        SignupResponse user = signup("record_01");
        var categories = categoryService.getCategories(user.userId(), true);
        var income = categories.stream().filter(c -> c.transactionType() == TransactionType.INCOME).findFirst().orElseThrow();
        var expense = categories.stream().filter(c -> c.transactionType() == TransactionType.EXPENSE).findFirst().orElseThrow();

        var created = recordService.create(user.userId(), new CreateFinancialRecordRequest(
                income.categoryId(), LocalDate.of(2026, 8, 1), new BigDecimal("10000"), "처음"));
        var updated = recordService.update(user.userId(), created.recordId(), new UpdateFinancialRecordRequest(
                expense.categoryId(), LocalDate.of(2026, 8, 2), new BigDecimal("20000"), "수정"));

        assertThat(updated.categoryId()).isEqualTo(expense.categoryId());
        assertThat(updated.amount()).isEqualByComparingTo("20000");
        recordService.delete(user.userId(), created.recordId());
        assertThatThrownBy(() -> recordService.get(user.userId(), created.recordId()))
                .isInstanceOf(FinancialRecordNotFoundException.class);
    }

    @Test
    void searchSupportsFiltersLatestOrderAndPaging() {
        SignupResponse user = signup("record_02");
        var income = categoryService.getCategories(user.userId(), true).stream()
                .filter(c -> c.transactionType() == TransactionType.INCOME).findFirst().orElseThrow();
        recordService.create(user.userId(), request(income.categoryId(), "2026-08-01", "100"));
        recordService.create(user.userId(), request(income.categoryId(), "2026-08-03", "300"));
        recordService.create(user.userId(), request(income.categoryId(), "2026-08-02", "200"));

        var result = recordService.search(user.userId(), LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31), income.categoryId(), TransactionType.INCOME,
                0, 2, "recordDate,desc");

        assertThat(result.content()).extracting("recordDate")
                .containsExactly(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 2));
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void inactiveAndOtherUsersCategoriesCannotBeUsed() {
        SignupResponse owner = signup("record_03");
        SignupResponse other = signup("record_04");
        var category = categoryService.getCategories(owner.userId(), true).getFirst();
        categoryService.deactivate(owner.userId(), category.categoryId());

        assertThatThrownBy(() -> recordService.create(owner.userId(),
                request(category.categoryId(), "2026-08-01", "100")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_INACTIVE));
        assertThatThrownBy(() -> recordService.create(other.userId(),
                request(category.categoryId(), "2026-08-01", "100")))
                .isInstanceOf(com.example.sales_summery.global.exception.CategoryNotFoundException.class);
    }

    @Test
    void otherUsersRecordLooksNotFoundAndInvalidRangeIsRejected() {
        SignupResponse owner = signup("record_05");
        SignupResponse other = signup("record_06");
        Long categoryId = categoryService.getCategories(owner.userId(), true).getFirst().categoryId();
        var record = recordService.create(owner.userId(), request(categoryId, "2026-08-01", "100"));

        assertThatThrownBy(() -> recordService.get(other.userId(), record.recordId()))
                .isInstanceOf(FinancialRecordNotFoundException.class);
        assertThatThrownBy(() -> recordService.search(owner.userId(), LocalDate.of(2026, 8, 2),
                LocalDate.of(2026, 8, 1), null, null, 0, 20, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATE_RANGE));
    }

    private CreateFinancialRecordRequest request(Long categoryId, String date, String amount) {
        return new CreateFinancialRecordRequest(categoryId, LocalDate.parse(date), new BigDecimal(amount), null);
    }

    private SignupResponse signup(String loginId) {
        return signupService.signup(new SignupRequest(loginId, "password123", "가게"));
    }
}
