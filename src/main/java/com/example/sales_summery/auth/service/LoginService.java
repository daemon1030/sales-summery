package com.example.sales_summery.auth.service;

import com.example.sales_summery.auth.dto.LoginRequest;
import com.example.sales_summery.auth.dto.LoginResponse;
import com.example.sales_summery.auth.security.JwtTokenService;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.domain.UserStatus;
import com.example.sales_summery.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.loginId().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!user.matchesPassword(request.password(), passwordEncoder)) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        validateStatus(user.getStatus());
        String token = jwtTokenService.createAccessToken(user);
        return LoginResponse.bearer(token, jwtTokenService.expirationSeconds());
    }

    private void validateStatus(UserStatus status) {
        if (status == UserStatus.SUSPENDED) throw new BusinessException(ErrorCode.USER_SUSPENDED);
        if (status == UserStatus.WITHDRAWN) throw new BusinessException(ErrorCode.USER_WITHDRAWN);
    }
}
