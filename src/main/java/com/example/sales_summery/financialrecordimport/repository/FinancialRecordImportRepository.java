package com.example.sales_summery.financialrecordimport.repository;

import com.example.sales_summery.financialrecordimport.domain.FinancialRecordImport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialRecordImportRepository extends JpaRepository<FinancialRecordImport, Long> {
    boolean existsByUserUserIdAndFileHash(Long userId, String fileHash);
}
