package com.example.sales_summery.financialrecordimport.controller;

import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.financialrecordimport.dto.ConfirmImportRequest;
import com.example.sales_summery.financialrecordimport.dto.ImportAnalysisResponse;
import com.example.sales_summery.financialrecordimport.dto.ImportResultResponse;
import com.example.sales_summery.financialrecordimport.service.FinancialRecordImportService;
import com.example.sales_summery.financialrecordimport.service.FinancialRecordImportTemplateService;
import com.example.sales_summery.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/financial-record-imports")
public class FinancialRecordImportController {
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final FinancialRecordImportService importService;
    private final FinancialRecordImportTemplateService templateService;

    public FinancialRecordImportController(FinancialRecordImportService importService,
                                           FinancialRecordImportTemplateService templateService) {
        this.importService = importService;
        this.templateService = templateService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("가계부_기본_양식.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(templateService.createTemplate());
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImportAnalysisResponse> analyze(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.success(importService.analyze(user.userId(), files));
    }

    @PostMapping("/{importToken}/confirm")
    public ApiResponse<ImportResultResponse> confirm(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable String importToken,
            @Valid @RequestBody ConfirmImportRequest request) {
        return ApiResponse.success(importService.confirm(user.userId(), importToken, request));
    }
}
