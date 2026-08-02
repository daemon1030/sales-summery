package com.example.sales_summery.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.category.repository.FinancialCategoryRepository;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.user.repository.UserRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SignupIntegrationTest {

    @Autowired SignupService signupService;
    @Autowired UserRepository userRepository;
    @Autowired FinancialCategoryRepository categoryRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired Validator validator;

    // 회원가입은 비밀번호를 암호화하고 기본 항목 5개를 함께 생성해야 한다.
    @Test
    void signupCreatesUserAndFiveDefaultCategories() {
        SignupResponse response = signupService.signup(
                new SignupRequest("STORE_01", "password123", "가게")
        );

        String passwordHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE user_id = ?", String.class, response.userId());

        assertThat(response.loginId()).isEqualTo("store_01");
        assertThat(passwordHash).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", passwordHash)).isTrue();
        assertThat(categoryRepository.findAllByUserUserIdOrderByCategoryNameAsc(response.userId()))
                .extracting("categoryName")
                .containsExactlyInAnyOrder("카드 매출", "현금 매출", "재료비", "인건비", "월세");
    }

    // 대소문자가 달라도 같은 로그인 아이디로 취급한다.
    @Test
    void duplicateLoginIdIsRejected() {
        signupService.signup(new SignupRequest("store_02", "password123", "가게"));

        assertThatThrownBy(() -> signupService.signup(
                new SignupRequest("STORE_02", "password456", "다른 가게")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.DUPLICATE_LOGIN_ID));
    }

    // 요청 DTO 단계에서 8자 미만 비밀번호를 차단한다.
    @Test
    void shortPasswordIsRejectedByRequestValidation() {
        SignupRequest request = new SignupRequest("store_03", "short", "가게");
        assertThat(validator.validate(request)).extracting("message")
                .contains("비밀번호는 8자 이상 72자 이하여야 합니다.");
    }
}
