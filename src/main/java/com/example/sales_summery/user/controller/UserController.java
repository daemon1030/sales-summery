package com.example.sales_summery.user.controller;

import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.global.response.ApiResponse;
import com.example.sales_summery.global.response.MessageResponse;
import com.example.sales_summery.user.dto.ChangeNameRequest;
import com.example.sales_summery.user.dto.ChangePasswordRequest;
import com.example.sales_summery.user.dto.SettlementStartDayRequest;
import com.example.sales_summery.user.dto.SettlementStartDayResponse;
import com.example.sales_summery.user.dto.UserResponse;
import com.example.sales_summery.user.dto.WithdrawRequest;
import com.example.sales_summery.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // userId는 요청에서 받지 않고 인증 principal에서만 가져온다.
    @GetMapping
    public ApiResponse<UserResponse> getMe(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userService.getMe(user.userId()));
    }

    @PatchMapping("/name")
    public ApiResponse<UserResponse> changeName(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody ChangeNameRequest request) {
        return ApiResponse.success(userService.changeName(user.userId(), request));
    }

    @GetMapping("/settlement-start-day")
    public ApiResponse<SettlementStartDayResponse> getSettlementStartDay(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(userService.getSettlementStartDay(user.userId()));
    }

    @PatchMapping("/settlement-start-day")
    public ApiResponse<SettlementStartDayResponse> changeSettlementStartDay(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SettlementStartDayRequest request) {
        return ApiResponse.success(userService.changeSettlementStartDay(user.userId(), request));
    }

    @PatchMapping("/password")
    public ApiResponse<MessageResponse> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                                       @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.userId(), request);
        return ApiResponse.success(new MessageResponse("비밀번호가 변경되었습니다."));
    }

    // 초기 버전은 즉시 삭제하지 않고 상태와 탈퇴 시각만 변경한다.
    @DeleteMapping
    public ApiResponse<MessageResponse> withdraw(@AuthenticationPrincipal AuthenticatedUser user,
                                                 @Valid @RequestBody WithdrawRequest request) {
        userService.withdraw(user.userId(), request);
        return ApiResponse.success(new MessageResponse("회원 탈퇴가 완료되었습니다."));
    }
}
