package com.example.sales_summery.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.sales_summery.auth.dto.LoginRequest;
import com.example.sales_summery.auth.dto.LoginResponse;
import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.auth.security.JwtTokenService;
import com.example.sales_summery.auth.service.LoginService;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(LoginSecurityIntegrationTest.ProtectedTestController.class)
class LoginSecurityIntegrationTest {

    @Autowired SignupService signupService;
    @Autowired LoginService loginService;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired UserRepository userRepository;
    @Autowired MockMvc mockMvc;

    @Test
    void loginReturnsOneHourAccessToken() {
        signupService.signup(new SignupRequest("login_01", "password123", "가게"));

        LoginResponse response = loginService.login(new LoginRequest("LOGIN_01", "password123"));
        AuthenticatedUser principal = jwtTokenService.parse(response.accessToken());

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600);
        assertThat(principal.loginId()).isEqualTo("login_01");
    }

    // 아이디 존재 여부와 관계없이 같은 오류 코드와 메시지를 사용한다.
    @Test
    void invalidCredentialsUseGeneralizedError() {
        signupService.signup(new SignupRequest("login_02", "password123", "가게"));

        assertInvalidCredentials(new LoginRequest("login_02", "wrong-password"));
        assertInvalidCredentials(new LoginRequest("unknown", "wrong-password"));
    }

    @Test
    void protectedApiUsesAuthenticatedPrincipal() throws Exception {
        var signup = signupService.signup(new SignupRequest("login_03", "password123", "가게"));
        String token = loginService.login(new LoginRequest("login_03", "password123")).accessToken();

        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(signup.userId()));
    }

    @Test
    void missingAndInvalidTokensReturnCommonUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void withdrawnUserCannotLoginOrReuseExistingToken() throws Exception {
        var signup = signupService.signup(new SignupRequest("login_04", "password123", "가게"));
        String token = loginService.login(new LoginRequest("login_04", "password123")).accessToken();
        User user = userRepository.findById(signup.userId()).orElseThrow();
        user.withdraw();
        userRepository.saveAndFlush(user);

        assertThatThrownBy(() -> loginService.login(new LoginRequest("login_04", "password123")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN));
        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private void assertInvalidCredentials(LoginRequest request) {
        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @RestController
    static class ProtectedTestController {
        @GetMapping("/api/v1/test/me")
        public com.example.sales_summery.global.response.ApiResponse<Long> me(
                @AuthenticationPrincipal AuthenticatedUser user) {
            return com.example.sales_summery.global.response.ApiResponse.success(user.userId());
        }
    }
}
