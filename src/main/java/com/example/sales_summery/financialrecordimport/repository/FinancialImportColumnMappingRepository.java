package com.example.sales_summery.financialrecordimport.repository;

import com.example.sales_summery.financialrecordimport.domain.FinancialImportColumnMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialImportColumnMappingRepository extends JpaRepository<FinancialImportColumnMapping, Long> {
    List<FinancialImportColumnMapping> findAllByUserUserId(Long userId);
    Optional<FinancialImportColumnMapping> findByUserUserIdAndNormalizedHeader(Long userId, String normalizedHeader);
}
