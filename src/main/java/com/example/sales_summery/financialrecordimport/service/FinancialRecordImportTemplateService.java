package com.example.sales_summery.financialrecordimport.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.DataValidationConstraint;
import org.apache.poi.ss.usermodel.DataValidationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class FinancialRecordImportTemplateService {
    private static final String[] HEADERS = {
            "날짜", "카드 매출", "현금 매출", "재료비", "인건비", "월세", "특이사항"
    };

    public byte[] createTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            Sheet input = workbook.createSheet("일별장부");
            buildSheet(input, styles, false);
            Sheet example = workbook.createSheet("작성예시");
            buildSheet(example, styles, true);
            workbook.setActiveSheet(0);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("엑셀 기본 양식을 만들 수 없습니다.", exception);
        }
    }

    private void buildSheet(Sheet sheet, Styles styles, boolean withExample) {
        sheet.setDisplayGridlines(false);
        sheet.createFreezePane(1, 3);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
        Row title = sheet.createRow(0);
        title.setHeightInPoints(32);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(withExample ? "가계부 작성 예시" : "가계부 엑셀 가져오기 양식");
        titleCell.setCellStyle(styles.title);

        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, HEADERS.length - 1));
        Row guide = sheet.createRow(1);
        guide.setHeightInPoints(27);
        Cell guideCell = guide.createCell(0);
        guideCell.setCellValue(withExample
                ? "이 시트는 작성 방법을 보여주는 예시이며, 실제 업로드 데이터는 '일별장부' 시트에 입력하세요."
                : "날짜별로 한 줄씩 입력하세요. 숫자와 숫자 결과를 반환하는 수식은 저장하며 빈칸·0·문자열은 저장하지 않습니다.");
        guideCell.setCellStyle(styles.guide);

        Row header = sheet.createRow(2);
        header.setHeightInPoints(26);
        for (int column = 0; column < HEADERS.length; column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(HEADERS[column]);
            cell.setCellStyle(column == 0 || column == 6 ? styles.headerNeutral
                    : column <= 2 ? styles.headerIncome : styles.headerExpense);
        }

        if (withExample) {
            Row row = sheet.createRow(3);
            Cell date = row.createCell(0);
            date.setCellValue(LocalDate.of(2026, 8, 1));
            date.setCellStyle(styles.date);
            double[] amounts = {320000, 85000, 74000, 0, 0};
            for (int index = 0; index < amounts.length; index++) {
                Cell amount = row.createCell(index + 1);
                amount.setCellValue(amounts[index]);
                amount.setCellStyle(styles.amount);
            }
            Cell note = row.createCell(6);
            note.setCellValue("비가 많이 와서 손님이 적었음");
            note.setCellStyle(styles.text);
        } else {
            for (int rowIndex = 3; rowIndex < 23; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellStyle(styles.date);
                for (int column = 1; column <= 5; column++) row.createCell(column).setCellStyle(styles.amount);
                row.createCell(6).setCellStyle(styles.text);
            }
            addValidations(sheet);
        }

        sheet.setAutoFilter(new CellRangeAddress(2, withExample ? 3 : 502, 0, HEADERS.length - 1));
        int[] widths = {14, 16, 16, 16, 16, 16, 34};
        for (int column = 0; column < widths.length; column++) sheet.setColumnWidth(column, widths[column] * 256);
        sheet.setZoom(95);
    }

    private void addValidations(Sheet sheet) {
        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint dateConstraint = helper.createDateConstraint(
                DataValidationConstraint.OperatorType.BETWEEN,
                "DATE(2000,1,1)", "DATE(2100,12,31)", "yyyy-mm-dd");
        addValidation(sheet, helper.createValidation(dateConstraint, new CellRangeAddressList(3, 502, 0, 0)),
                "날짜 오류", "2000-01-01부터 2100-12-31 사이의 날짜를 입력해주세요.");

        DataValidationConstraint amountConstraint = helper.createDecimalConstraint(
                DataValidationConstraint.OperatorType.GREATER_THAN, "0", null);
        addValidation(sheet, helper.createValidation(amountConstraint, new CellRangeAddressList(3, 502, 1, 5)),
                "금액 오류", "빈칸 또는 0보다 큰 금액을 입력해주세요.");
    }

    private void addValidation(Sheet sheet, DataValidation validation, String title, String message) {
        validation.setEmptyCellAllowed(true);
        validation.setShowErrorBox(true);
        validation.createErrorBox(title, message);
        sheet.addValidationData(validation);
    }

    private static final class Styles {
        private final CellStyle title;
        private final CellStyle guide;
        private final CellStyle headerNeutral;
        private final CellStyle headerIncome;
        private final CellStyle headerExpense;
        private final CellStyle date;
        private final CellStyle amount;
        private final CellStyle text;

        private Styles(XSSFWorkbook workbook) {
            title = style(workbook, "#173F35", IndexedColors.WHITE, true, 16);
            title.setAlignment(HorizontalAlignment.LEFT);
            title.setVerticalAlignment(VerticalAlignment.CENTER);

            guide = style(workbook, "#F3F1E9", IndexedColors.DARK_GREEN, false, 10);
            guide.setAlignment(HorizontalAlignment.LEFT);
            guide.setVerticalAlignment(VerticalAlignment.CENTER);

            headerNeutral = headerStyle(workbook, "#E5D28E", IndexedColors.DARK_GREEN);
            headerIncome = headerStyle(workbook, "#DCEBE3", IndexedColors.DARK_GREEN);
            headerExpense = headerStyle(workbook, "#F3DED7", IndexedColors.DARK_RED);

            date = bodyStyle(workbook);
            date.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            date.setAlignment(HorizontalAlignment.CENTER);

            amount = bodyStyle(workbook);
            amount.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            amount.setAlignment(HorizontalAlignment.RIGHT);

            text = bodyStyle(workbook);
            text.setAlignment(HorizontalAlignment.LEFT);
        }

        private static CellStyle style(XSSFWorkbook workbook, String fillColor, IndexedColors fontColor,
                                       boolean bold, int fontSize) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(new XSSFColor(java.awt.Color.decode(fillColor), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setFontName("맑은 고딕");
            font.setFontHeightInPoints((short) fontSize);
            font.setBold(bold);
            font.setColor(fontColor.getIndex());
            style.setFont(font);
            return style;
        }

        private static CellStyle headerStyle(XSSFWorkbook workbook, String fillColor, IndexedColors fontColor) {
            CellStyle style = style(workbook, fillColor, fontColor, true, 10);
            style.setAlignment(HorizontalAlignment.CENTER);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderBottom(BorderStyle.MEDIUM);
            style.setBottomBorderColor(IndexedColors.GREY_40_PERCENT.getIndex());
            return style;
        }

        private static CellStyle bodyStyle(XSSFWorkbook workbook) {
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setFontName("맑은 고딕");
            font.setFontHeightInPoints((short) 10);
            style.setFont(font);
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            return style;
        }
    }
}
