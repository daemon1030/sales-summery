package com.example.sales_summery.auth.security;

// 보호 API는 요청 DTO의 userId 대신 SecurityContext의 이 객체를 사용한다.
public record AuthenticatedUser(Long userId, String loginId) {
}
