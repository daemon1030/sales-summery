package com.example.sales_summery.auth.controller;

import com.example.sales_summery.auth.dto.LoginRequest;
import com.example.sales_summery.auth.dto.LoginResponse;
import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.LoginService;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SignupService signupService;
    private final LoginService loginService;

    public AuthController(SignupService signupService, LoginService loginService) {
        this.signupService = signupService;
        this.loginService = loginService;
    }

    // 생성 API 원칙에 따라 회원가입 성공 시 201 Created를 반환한다.
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(signupService.signup(request)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(loginService.login(request));
    }
}
