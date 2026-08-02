package com.example.sales_summery.financialrecord.controller;

import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.financialrecord.dto.CreateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.dto.FinancialRecordResponse;
import com.example.sales_summery.financialrecord.dto.UpdateFinancialRecordRequest;
import com.example.sales_summery.financialrecord.service.FinancialRecordService;
import com.example.sales_summery.global.response.ApiResponse;
import com.example.sales_summery.global.response.MessageResponse;
import com.example.sales_summery.global.response.PageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/financial-records")
public class FinancialRecordController {
    private final FinancialRecordService recordService;

    public FinancialRecordController(FinancialRecordService recordService) {
        this.recordService = recordService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateFinancialRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(recordService.create(user.userId(), request)));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<FinancialRecordResponse> get(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long recordId) {
        return ApiResponse.success(recordService.get(user.userId(), recordId));
    }

    @PatchMapping("/{recordId}")
    public ApiResponse<FinancialRecordResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long recordId, @Valid @RequestBody UpdateFinancialRecordRequest request) {
        return ApiResponse.success(recordService.update(user.userId(), recordId, request));
    }

    @DeleteMapping("/{recordId}")
    public ApiResponse<MessageResponse> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable Long recordId) {
        recordService.delete(user.userId(), recordId);
        return ApiResponse.success(new MessageResponse("수입·지출 기록이 삭제되었습니다."));
    }

    @GetMapping
    public ApiResponse<PageResponse<FinancialRecordResponse>> search(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "recordDate,desc") String sort) {
        return ApiResponse.success(recordService.search(user.userId(), startDate, endDate,
                categoryId, transactionType, page, size, sort));
    }
}
