package com.example.sales_summery.auth.dto;

import jakarta.validation.constraints.NotBlank;

// 로그인 실패 시 아이디와 비밀번호 중 어느 값이 틀렸는지는 외부에 노출하지 않는다.
public record LoginRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.") String loginId,
        @NotBlank(message = "비밀번호는 필수입니다.") String password
) {
}
