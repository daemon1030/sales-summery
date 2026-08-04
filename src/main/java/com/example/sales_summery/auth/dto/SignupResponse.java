package com.example.sales_summery.auth.dto;

import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.domain.UserStatus;

// 비밀번호 해시와 같은 내부 정보는 응답 DTO에 포함하지 않는다.
public record SignupResponse(
        Long userId,
        String loginId,
        String name,
        UserStatus status,
        int settlementStartDay
) {
    public static SignupResponse from(User user) {
        return new SignupResponse(user.getUserId(), user.getLoginId(), user.getName(),
                user.getStatus(), user.getSettlementStartDay());
    }
}
