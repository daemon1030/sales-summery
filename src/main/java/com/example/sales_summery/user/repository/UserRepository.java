package com.example.sales_summery.user.repository;

import com.example.sales_summery.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 아이디로 사용자 조회
    Optional<User> findByLoginId(String loginId);

    // 회원가입 전 로그인 아이디 중복 확인
    boolean existsByLoginId(String loginId);
}
