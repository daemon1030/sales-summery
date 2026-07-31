package com.example.sales_summery.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.category.domain.Frequency;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.repository.FinancialCategoryRepository;
import com.example.sales_summery.dailynote.domain.DailyNote;
import com.example.sales_summery.dailynote.repository.DailyNoteRepository;
import com.example.sales_summery.financialrecord.domain.FinancialRecord;
import com.example.sales_summery.financialrecord.repository.FinancialRecordRepository;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FinancialCategoryRepository categoryRepository;

    @Autowired
    private FinancialRecordRepository recordRepository;

    @Autowired
    private DailyNoteRepository dailyNoteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void settlementStartDayAllowsOnlyOneThroughTwentyEight() {
        assertThatThrownBy(() -> User.create("store_01", "encoded-password", "가게", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.create("store_01", "encoded-password", "가게", 29))
                .isInstanceOf(IllegalArgumentException.class);

        User user = userRepository.saveAndFlush(User.create("Store_01", "encoded-password", "가게", 28));

        assertThat(user.getLoginId()).isEqualTo("store_01");
        assertThat(user.getSettlementStartDay()).isEqualTo(28);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    void loginIdAllowsOnlyFourToTenLettersNumbersAndUnderscore() {
        assertThatThrownBy(() -> User.create("abc", "encoded-password", "가게", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.create("store_00001", "encoded-password", "가게", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.create("store-01", "encoded-password", "가게", 1))
                .isInstanceOf(IllegalArgumentException.class);

        User user = userRepository.saveAndFlush(User.create("STORE_0001", "encoded-password", "가게", 1));

        assertThat(user.getLoginId()).isEqualTo("store_0001");
    }

    @Test
    void settlementStartDayHasDatabaseDefaultValueOne() {
        jdbcTemplate.update(
                """
                INSERT INTO users (login_id, password_hash, name, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                "default_01",
                "encoded-password",
                "가게",
                "ACTIVE"
        );

        Integer settlementStartDay = jdbcTemplate.queryForObject(
                "SELECT settlement_start_day FROM users WHERE login_id = ?",
                Integer.class,
                "default_01"
        );

        assertThat(settlementStartDay).isEqualTo(1);
    }

    @Test
    void categoryNameIsUniqueWithinTheSameUser() {
        User user = saveUser("store_01");
        categoryRepository.saveAndFlush(incomeCategory(user, "카드 매출"));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(incomeCategory(user, "카드 매출")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void differentUsersCanUseTheSameCategoryName() {
        User firstUser = saveUser("store_01");
        User secondUser = saveUser("store_02");

        categoryRepository.saveAndFlush(incomeCategory(firstUser, "카드 매출"));
        categoryRepository.saveAndFlush(incomeCategory(secondUser, "카드 매출"));

        assertThat(categoryRepository.existsByUserUserIdAndCategoryName(firstUser.getUserId(), "카드 매출"))
                .isTrue();
        assertThat(categoryRepository.existsByUserUserIdAndCategoryName(secondUser.getUserId(), "카드 매출"))
                .isTrue();
    }

    @Test
    void invalidIncomeCostTypeIsRejected() {
        User user = saveUser("store_01");

        assertThatThrownBy(() -> FinancialCategory.create(
                user,
                "잘못된 수입 항목",
                TransactionType.INCOME,
                CostType.FIXED,
                Frequency.DAILY
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multipleRecordsForTheSameDateAndCategoryAreAllowed() {
        User user = saveUser("store_01");
        FinancialCategory category = categoryRepository.saveAndFlush(incomeCategory(user, "카드 매출"));
        LocalDate recordDate = LocalDate.of(2026, 7, 31);

        recordRepository.saveAndFlush(FinancialRecord.create(
                category, recordDate, new BigDecimal("10000.00"), "오전 매출"
        ));
        recordRepository.saveAndFlush(FinancialRecord.create(
                category, recordDate, new BigDecimal("20000.00"), "오후 매출"
        ));

        assertThat(recordRepository.findAllByCategoryUserUserIdAndRecordDateBetweenOrderByRecordDateDesc(
                user.getUserId(), recordDate, recordDate
        )).hasSize(2);
    }

    @Test
    void recordLookupIsScopedToTheOwner() {
        User owner = saveUser("store_01");
        User otherUser = saveUser("store_02");
        FinancialCategory category = categoryRepository.saveAndFlush(incomeCategory(owner, "카드 매출"));
        FinancialRecord record = recordRepository.saveAndFlush(FinancialRecord.create(
                category, LocalDate.of(2026, 7, 31), new BigDecimal("10000.00"), null
        ));

        assertThat(recordRepository.findByRecordIdAndCategoryUserUserId(record.getRecordId(), owner.getUserId()))
                .isPresent();
        assertThat(recordRepository.findByRecordIdAndCategoryUserUserId(record.getRecordId(), otherUser.getUserId()))
                .isEmpty();
    }

    @Test
    void duplicateDailyNoteForTheSameUserAndDateIsRejected() {
        User user = saveUser("store_01");
        LocalDate noteDate = LocalDate.of(2026, 7, 31);
        dailyNoteRepository.saveAndFlush(DailyNote.create(user, noteDate, "비가 많이 옴"));

        assertThatThrownBy(() -> dailyNoteRepository.saveAndFlush(DailyNote.create(user, noteDate, "추가 메모")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void blankDailyNoteAndNonPositiveAmountAreRejected() {
        User user = saveUser("store_01");
        FinancialCategory category = categoryRepository.saveAndFlush(incomeCategory(user, "카드 매출"));

        assertThatThrownBy(() -> DailyNote.create(user, LocalDate.of(2026, 7, 31), "   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FinancialRecord.create(
                category, LocalDate.of(2026, 7, 31), BigDecimal.ZERO, null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private User saveUser(String loginId) {
        return userRepository.saveAndFlush(User.create(loginId, "encoded-password", "가게", 1));
    }

    private FinancialCategory incomeCategory(User user, String categoryName) {
        return FinancialCategory.create(
                user,
                categoryName,
                TransactionType.INCOME,
                CostType.NONE,
                Frequency.DAILY
        );
    }
}
