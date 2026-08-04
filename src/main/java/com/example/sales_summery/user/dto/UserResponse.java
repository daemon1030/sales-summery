package com.example.sales_summery.user.dto;

import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.domain.UserStatus;

public record UserResponse(Long userId, String loginId, String name, UserStatus status,
                           int settlementStartDay) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getUserId(), user.getLoginId(), user.getName(),
                user.getStatus(), user.getSettlementStartDay());
    }
}
