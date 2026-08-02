package com.example.sales_summery.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입 API 입력값은 Controller 진입 시점에 형식과 필수값을 검증한다.
public record SignupRequest(
        @NotBlank(message = "로그인 아이디는 필수입니다.")
        @Size(min = 4, max = 10, message = "로그인 아이디는 4자 이상 10자 이하여야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "로그인 아이디는 영문자, 숫자, 밑줄만 사용할 수 있습니다.")
        String loginId,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
        String password,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name
) {
}
