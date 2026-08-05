package com.example.sales_summery.financialrecordimport;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.Frequency;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.service.CategoryService;
import com.example.sales_summery.dailynote.service.DailyNoteService;
import com.example.sales_summery.financialrecord.service.FinancialRecordService;
import com.example.sales_summery.financialrecordimport.dto.ConfirmImportRequest;
import com.example.sales_summery.financialrecordimport.dto.ImportColumnMappingRequest;
import com.example.sales_summery.financialrecordimport.dto.ImportFileStatus;
import com.example.sales_summery.financialrecordimport.dto.ImportMappingAction;
import com.example.sales_summery.financialrecordimport.service.FinancialRecordImportService;
import com.example.sales_summery.financialrecordimport.service.FinancialRecordImportTemplateService;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FinancialRecordImportIntegrationTest {
    @Autowired SignupService signupService;
    @Autowired CategoryService categoryService;
    @Autowired FinancialRecordService recordService;
    @Autowired DailyNoteService noteService;
    @Autowired FinancialRecordImportService importService;
    @Autowired FinancialRecordImportTemplateService templateService;

    @Test
    void templateContainsBlankInputSheetAndExampleSheet() throws Exception {
        try (Workbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(templateService.createTemplate()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheetAt(0).getSheetName()).isEqualTo("일별장부");
            assertThat(workbook.getSheetAt(1).getSheetName()).isEqualTo("작성예시");
            Row header = workbook.getSheetAt(0).getRow(2);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("날짜");
            assertThat(header.getCell(6).getStringCellValue()).isEqualTo("특이사항");
            assertThat(workbook.getSheetAt(0).getRow(3).getCell(0).getStringCellValue()).isBlank();
        }
    }

    @Test
    void knownColumnsAreImportedWithoutMappingAndDuplicateFileIsDetected() throws Exception {
        SignupResponse user = signup("excel_01");
        MockMultipartFile file = workbook("기본.xlsx",
                new String[]{"날짜", "카드 매출", "현금 매출", "재료비", "인건비", "월세", "특이사항"},
                new Object[]{LocalDate.of(2026, 8, 1), 300000, 100000, 50000, 0, 0, "비가 많이 옴"});

        var analysis = importService.analyze(user.userId(), List.of(file));
        assertThat(analysis.requiresMapping()).isFalse();
        assertThat(analysis.readyFileCount()).isEqualTo(1);

        var result = importService.confirm(user.userId(), analysis.importToken(), new ConfirmImportRequest(List.of()));
        assertThat(result.createdRecordCount()).isEqualTo(3);
        assertThat(result.createdNoteCount()).isEqualTo(1);
        assertThat(recordService.search(user.userId(), LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 1), null, null, 0, 20, null).totalElements()).isEqualTo(3);
        assertThat(noteService.getByDate(user.userId(), LocalDate.of(2026, 8, 1)).content())
                .isEqualTo("비가 많이 옴");

        var duplicate = importService.analyze(user.userId(), List.of(file));
        assertThat(duplicate.files().getFirst().status()).isEqualTo(ImportFileStatus.DUPLICATE);
    }

    @Test
    void unknownColumnCanCreateCategoryBeforeImport() throws Exception {
        SignupResponse user = signup("excel_02");
        MockMultipartFile file = workbook("배달.xlsx",
                new String[]{"날짜", "카드 매출", "배달 매출"},
                new Object[]{LocalDate.of(2026, 8, 2), 200000, 120000});

        var analysis = importService.analyze(user.userId(), List.of(file));
        assertThat(analysis.requiresMapping()).isTrue();
        assertThat(analysis.columns()).anySatisfy(column -> {
            assertThat(column.header()).isEqualTo("배달 매출");
            assertThat(column.valueCount()).isEqualTo(1);
        });

        var mapping = new ImportColumnMappingRequest("배달 매출", ImportMappingAction.CREATE, null,
                "배달 매출", TransactionType.INCOME, CostType.NONE, Frequency.DAILY);
        var result = importService.confirm(user.userId(), analysis.importToken(),
                new ConfirmImportRequest(List.of(mapping)));

        assertThat(result.createdCategoryCount()).isEqualTo(1);
        assertThat(result.createdRecordCount()).isEqualTo(2);
        assertThat(categoryService.getCategories(user.userId(), true))
                .anySatisfy(category -> assertThat(category.categoryName()).isEqualTo("배달 매출"));
    }

    @Test
    void existingCategoryAliasIsRememberedForNextBatch() throws Exception {
        SignupResponse user = signup("excel_03");
        var cardCategory = categoryService.getCategories(user.userId(), true).stream()
                .filter(category -> category.categoryName().equals("카드 매출")).findFirst().orElseThrow();
        MockMultipartFile first = workbook("카드1.xlsx", new String[]{"날짜", "카드"},
                new Object[]{LocalDate.of(2026, 8, 3), 100000});

        var firstAnalysis = importService.analyze(user.userId(), List.of(first));
        assertThat(firstAnalysis.requiresMapping()).isTrue();
        var mapping = new ImportColumnMappingRequest("카드", ImportMappingAction.EXISTING,
                cardCategory.categoryId(), null, null, null, null);
        importService.confirm(user.userId(), firstAnalysis.importToken(), new ConfirmImportRequest(List.of(mapping)));

        MockMultipartFile second = workbook("카드2.xlsx", new String[]{"날짜", "카드"},
                new Object[]{LocalDate.of(2026, 8, 4), 120000});
        var secondAnalysis = importService.analyze(user.userId(), List.of(second));

        assertThat(secondAnalysis.requiresMapping()).isFalse();
        assertThat(secondAnalysis.columns().getFirst().categoryId()).isEqualTo(cardCategory.categoryId());
    }

    @Test
    void amountColumnsIgnoreStringsIncludeNumericFormulasAndSkipRowsWithoutDates() throws Exception {
        SignupResponse user = signup("excel_04");
        MockMultipartFile file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("일별장부");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("날짜");
            header.createCell(1).setCellValue("카드 매출");
            header.createCell(2).setCellValue("현금 매출");
            header.createCell(3).setCellValue("재료비");
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(LocalDate.of(2026, 8, 5));
            row.createCell(1).setCellValue(100000);
            row.createCell(2).setCellValue("200000");
            row.createCell(3).setCellFormula("B2*2");
            Row totalRow = sheet.createRow(2);
            totalRow.createCell(1).setCellValue(999999);
            totalRow.createCell(2).setCellValue(888888);
            totalRow.createCell(3).setCellValue(777777);
            Row invalidTextDateRow = sheet.createRow(3);
            invalidTextDateRow.createCell(0).setCellValue("2025년");
            invalidTextDateRow.createCell(1).setCellValue(666666);
            Row invalidNumericDateRow = sheet.createRow(4);
            invalidNumericDateRow.createCell(0).setCellValue(2025);
            invalidNumericDateRow.createCell(1).setCellValue(555555);
            workbook.write(output);
            file = new MockMultipartFile("files", "숫자만.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }

        var analysis = importService.analyze(user.userId(), List.of(file));

        assertThat(analysis.files().getFirst().status()).isEqualTo(ImportFileStatus.READY);
        assertThat(analysis.files().getFirst().recordCount()).isEqualTo(2);
        assertThat(analysis.files().getFirst().errors()).isEmpty();
        assertThat(analysis.columns()).extracting("header").containsExactly("카드 매출", "재료비");
        assertThat(analysis.columns().stream().filter(column -> column.header().equals("재료비"))
                .findFirst().orElseThrow().totalAmount()).isEqualByComparingTo("200000.00");
    }

    private SignupResponse signup(String loginId) {
        return signupService.signup(new SignupRequest(loginId, "password123", "엑셀 사용자"));
    }

    private MockMultipartFile workbook(String fileName, String[] headers, Object[] values) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("일별장부");
            Row header = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) header.createCell(index).setCellValue(headers[index]);
            Row row = sheet.createRow(1);
            for (int index = 0; index < values.length; index++) {
                Object value = values[index];
                if (value instanceof LocalDate date) row.createCell(index).setCellValue(date);
                else if (value instanceof Number number) row.createCell(index).setCellValue(number.doubleValue());
                else row.createCell(index).setCellValue(String.valueOf(value));
            }
            workbook.write(output);
            return new MockMultipartFile("files", fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
