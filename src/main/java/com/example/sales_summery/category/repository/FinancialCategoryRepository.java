package com.example.sales_summery.category.repository;

import com.example.sales_summery.category.domain.FinancialCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialCategoryRepository extends JpaRepository<FinancialCategory, Long> {

    // 현재 사용자가 소유한 항목만 조회
    Optional<FinancialCategory> findByCategoryIdAndUserUserId(Long categoryId, Long userId);

    // 같은 사용자 안의 항목명 중복 확인
    boolean existsByUserUserIdAndCategoryName(Long userId, String categoryName);

    // 사용자 항목을 이름순으로 조회
    List<FinancialCategory> findAllByUserUserIdOrderByCategoryNameAsc(Long userId);

    // 신규 기록에 사용할 활성 항목만 조회
    List<FinancialCategory> findAllByUserUserIdAndActiveTrueOrderByCategoryNameAsc(Long userId);
}
