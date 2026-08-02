package com.example.sales_summery.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sales_summery.auth.dto.LoginRequest;
import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.LoginService;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.user.domain.UserStatus;
import com.example.sales_summery.user.dto.ChangeNameRequest;
import com.example.sales_summery.user.dto.ChangePasswordRequest;
import com.example.sales_summery.user.dto.SettlementStartDayRequest;
import com.example.sales_summery.user.dto.WithdrawRequest;
import com.example.sales_summery.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {
    @Autowired SignupService signupService;
    @Autowired LoginService loginService;
    @Autowired UserService userService;

    @Test
    void userCanReadAndChangeOwnProfileSettings() {
        SignupResponse signup = signupService.signup(
                new SignupRequest("profile_1", "password123", "이전 이름"));

        assertThat(userService.getMe(signup.userId()).name()).isEqualTo("이전 이름");
        assertThat(userService.changeName(signup.userId(), new ChangeNameRequest("새 이름")).name())
                .isEqualTo("새 이름");
        assertThat(userService.changeSettlementStartDay(signup.userId(),
                new SettlementStartDayRequest(28)).settlementStartDay()).isEqualTo(28);
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndReplacesHash() {
        SignupResponse signup = signupService.signup(
                new SignupRequest("profile_2", "password123", "가게"));

        assertThatThrownBy(() -> userService.changePassword(signup.userId(),
                new ChangePasswordRequest("wrong-password", "newpassword123")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PASSWORD));

        userService.changePassword(signup.userId(),
                new ChangePasswordRequest("password123", "newpassword123"));
        assertThatThrownBy(() -> loginService.login(new LoginRequest("profile_2", "password123")))
                .isInstanceOf(BusinessException.class);
        assertThat(loginService.login(new LoginRequest("profile_2", "newpassword123")).accessToken())
                .isNotBlank();
    }

    @Test
    void withdrawalRequiresPasswordAndChangesStatusWithoutDeletingUser() {
        SignupResponse signup = signupService.signup(
                new SignupRequest("profile_3", "password123", "가게"));

        userService.withdraw(signup.userId(), new WithdrawRequest("password123"));

        assertThat(userService.getMe(signup.userId()).status()).isEqualTo(UserStatus.WITHDRAWN);
        assertThatThrownBy(() -> loginService.login(new LoginRequest("profile_3", "password123")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN));
    }
}
