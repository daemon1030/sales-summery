package com.example.sales_summery.financialrecord.service;

import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.repository.FinancialCategoryRepository;
import com.example.sales_summery.financialrecord.domain.FinancialRecord;
import com.example.sales_summery.financialrecord.dto.CreateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.dto.FinancialRecordResponse;
import com.example.sales_summery.financialrecord.dto.UpdateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.repository.FinancialRecordRepository;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.CategoryNotFoundException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.FinancialRecordNotFoundException;
import com.example.sales_summery.global.response.PageResponse;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialRecordService {
    private static final Set<String> ALLOWED_SORTS = Set.of("recordDate", "amount", "createdAt");
    private final FinancialRecordRepository recordRepository;
    private final FinancialCategoryRepository categoryRepository;

    public FinancialRecordService(FinancialRecordRepository recordRepository,
                                  FinancialCategoryRepository categoryRepository) {
        this.recordRepository = recordRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public FinancialRecordResponse create(Long userId, CreateFinancialRecordRequest request) {
        FinancialCategory category = findActiveOwnedCategory(userId, request.categoryId());
        return FinancialRecordResponse.from(recordRepository.save(FinancialRecord.create(
                category, request.recordDate(), request.amount(), request.memo())));
    }

    @Transactional(readOnly = true)
    public FinancialRecordResponse get(Long userId, Long recordId) {
        return FinancialRecordResponse.from(findOwnedRecord(userId, recordId));
    }

    @Transactional
    public FinancialRecordResponse update(Long userId, Long recordId, UpdateFinancialRecordRequest request) {
        FinancialRecord record = findOwnedRecord(userId, recordId);
        FinancialCategory category = findActiveOwnedCategory(userId, request.categoryId());
        record.change(category, request.recordDate(), request.amount(), request.memo());
        return FinancialRecordResponse.from(record);
    }

    @Transactional
    public void delete(Long userId, Long recordId) {
        recordRepository.delete(findOwnedRecord(userId, recordId));
    }

    @Transactional(readOnly = true)
    public PageResponse<FinancialRecordResponse> search(Long userId, LocalDate startDate, LocalDate endDate,
            Long categoryId, TransactionType transactionType, int page, int size, String sort) {
        if (startDate.isAfter(endDate)) throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        if (page < 0 || size < 1 || size > 100) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        PageRequest pageable = PageRequest.of(page, size, parseSort(sort));
        Page<FinancialRecordResponse> result = recordRepository.search(userId, startDate, endDate,
                categoryId, transactionType, pageable).map(FinancialRecordResponse::from);
        return PageResponse.from(result);
    }

    private Sort parseSort(String value) {
        String[] parts = value == null ? new String[]{"recordDate", "desc"} : value.split(",");
        String property = parts[0];
        if (!ALLOWED_SORTS.contains(property)) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.DESC, "recordId"));
    }

    private FinancialRecord findOwnedRecord(Long userId, Long recordId) {
        return recordRepository.findByRecordIdAndCategoryUserUserId(recordId, userId)
                .orElseThrow(FinancialRecordNotFoundException::new);
    }

    private FinancialCategory findActiveOwnedCategory(Long userId, Long categoryId) {
        FinancialCategory category = categoryRepository.findByCategoryIdAndUserUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
        if (!category.isActive()) throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
        return category;
    }
}
