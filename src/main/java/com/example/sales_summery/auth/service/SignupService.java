package com.example.sales_summery.auth.service;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SignupService {

    private final UserRepository userRepository;
    private final DefaultCategoryCreator defaultCategoryCreator;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserRepository userRepository, DefaultCategoryCreator defaultCategoryCreator,
                         PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.defaultCategoryCreator = defaultCategoryCreator;
        this.passwordEncoder = passwordEncoder;
    }

    // 사용자와 기본 항목 중 하나라도 저장에 실패하면 전체 작업을 롤백한다.
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String normalizedLoginId = request.loginId().toLowerCase(Locale.ROOT);
        if (userRepository.existsByLoginId(normalizedLoginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        User user = userRepository.save(User.create(normalizedLoginId,
                passwordEncoder.encode(request.password()), request.name(), 1));
        defaultCategoryCreator.createFor(user);
        return SignupResponse.from(user);
    }
}
