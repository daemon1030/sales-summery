package com.example.sales_summery.user.service;

import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.UserNotFoundException;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.dto.ChangeNameRequest;
import com.example.sales_summery.user.dto.ChangePasswordRequest;
import com.example.sales_summery.user.dto.SettlementStartDayRequest;
import com.example.sales_summery.user.dto.SettlementStartDayResponse;
import com.example.sales_summery.user.dto.UserResponse;
import com.example.sales_summery.user.dto.WithdrawRequest;
import com.example.sales_summery.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse changeName(Long userId, ChangeNameRequest request) {
        User user = findUser(userId);
        user.changeName(request.name());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public SettlementStartDayResponse getSettlementStartDay(Long userId) {
        return new SettlementStartDayResponse(findUser(userId).getSettlementStartDay());
    }

    @Transactional
    public SettlementStartDayResponse changeSettlementStartDay(Long userId, SettlementStartDayRequest request) {
        User user = findUser(userId);
        user.changeSettlementStartDay(request.settlementStartDay());
        return new SettlementStartDayResponse(user.getSettlementStartDay());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        verifyPassword(user, request.currentPassword());
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void withdraw(Long userId, WithdrawRequest request) {
        User user = findUser(userId);
        verifyPassword(user, request.currentPassword());
        user.withdraw();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private void verifyPassword(User user, String rawPassword) {
        if (!user.matchesPassword(rawPassword, passwordEncoder)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
