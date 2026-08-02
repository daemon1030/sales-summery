package com.example.sales_summery.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.service.DefaultCategoryCreator;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SignupRollbackIntegrationTest {

    @Autowired SignupService signupService;
    @Autowired UserRepository userRepository;

    // 기본 항목 생성 실패 상황을 만들되 실제 SignupService 트랜잭션은 그대로 실행한다.
    @MockitoBean
    DefaultCategoryCreator defaultCategoryCreator;

    @Test
    void categoryCreationFailureRollsBackUserCreation() {
        doThrow(new IllegalStateException("기본 항목 생성 실패"))
                .when(defaultCategoryCreator).createFor(any(User.class));

        assertThatThrownBy(() -> signupService.signup(
                new SignupRequest("rollback", "password123", "가게")))
                .isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.existsByLoginId("rollback")).isFalse();
    }
}
