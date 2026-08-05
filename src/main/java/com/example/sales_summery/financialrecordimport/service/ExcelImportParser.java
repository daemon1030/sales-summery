package com.example.sales_summery.financialrecordimport.service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ExcelImportParser {
    private static final int HEADER_SEARCH_LIMIT = 20;
    private static final int MAX_ROWS = 50_000;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");
    private static final LocalDate MIN_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);
    private static final Set<String> DATE_HEADERS = Set.of("날짜", "일자", "거래일");
    private static final Set<String> NOTE_HEADERS = Set.of("특이사항", "메모", "비고");
    private static final Set<String> IGNORED_HEADERS = Set.of(
            "총수입", "총지출", "순이익", "합계", "남은돈",
            "토탈수입", "토탈재료대", "토탈인건비"
    );
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    );

    ParsedImportFile parse(String fileName, String fileHash, byte[] bytes) {
        List<String> errors = new ArrayList<>();
        List<ParsedImportRow> parsedRows = new ArrayList<>();
        Map<String, String> displayHeaders = new LinkedHashMap<>();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) {
                return invalid(fileName, fileHash, "시트가 없습니다.");
            }
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.KOREA);
            HeaderDefinition header = findHeader(sheet, formatter, evaluator, errors);
            if (header == null) {
                return new ParsedImportFile(fileName, fileHash, List.of(), Map.of(), List.copyOf(errors));
            }

            if (sheet.getLastRowNum() - header.rowIndex() > MAX_ROWS) {
                return invalid(fileName, fileHash, "한 파일에서 최대 50,000행까지 가져올 수 있습니다.");
            }

            displayHeaders.putAll(header.displayHeaders());
            for (int rowIndex = header.rowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                parseRow(row, header, formatter, evaluator, parsedRows, errors);
            }
        } catch (Exception exception) {
            errors.add("엑셀 파일을 읽을 수 없습니다: " + safeMessage(exception));
        }

        return new ParsedImportFile(fileName, fileHash, List.copyOf(parsedRows),
                Map.copyOf(displayHeaders), List.copyOf(errors));
    }

    static String normalizeHeader(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.KOREAN);
    }

    private HeaderDefinition findHeader(Sheet sheet, DataFormatter formatter,
                                        FormulaEvaluator evaluator, List<String> errors) {
        for (int rowIndex = sheet.getFirstRowNum();
             rowIndex <= Math.min(sheet.getLastRowNum(), sheet.getFirstRowNum() + HEADER_SEARCH_LIMIT); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            Map<Integer, String> headers = new LinkedHashMap<>();
            for (Cell cell : row) {
                String header = formatter.formatCellValue(cell, evaluator).trim();
                if (!header.isBlank()) headers.put(cell.getColumnIndex(), header);
            }
            Integer dateColumn = headers.entrySet().stream()
                    .filter(entry -> DATE_HEADERS.contains(normalizeHeader(entry.getValue())))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (dateColumn == null) continue;

            Integer noteColumn = headers.entrySet().stream()
                    .filter(entry -> NOTE_HEADERS.contains(normalizeHeader(entry.getValue())))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            Map<Integer, String> amountColumns = new LinkedHashMap<>();
            Map<String, String> displayHeaders = new LinkedHashMap<>();
            Set<String> seen = new LinkedHashSet<>();
            for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                String normalized = normalizeHeader(entry.getValue());
                if (entry.getKey().equals(dateColumn) || entry.getKey().equals(noteColumn)
                        || IGNORED_HEADERS.contains(normalized)) continue;
                if (!seen.add(normalized)) {
                    errors.add("헤더 행에 중복된 열이 있습니다: " + entry.getValue());
                    continue;
                }
                if (normalized.length() > 255) {
                    errors.add("열 이름은 255자 이하여야 합니다: " + entry.getValue());
                    continue;
                }
                amountColumns.put(entry.getKey(), normalized);
                displayHeaders.put(normalized, entry.getValue().trim());
            }
            if (amountColumns.isEmpty()) {
                errors.add("금액을 입력할 카테고리 열이 없습니다.");
            }
            return new HeaderDefinition(rowIndex, dateColumn, noteColumn, amountColumns, displayHeaders);
        }
        errors.add("첫 20행 안에서 '날짜' 열을 찾을 수 없습니다.");
        return null;
    }

    private void parseRow(Row row, HeaderDefinition header, DataFormatter formatter,
                          FormulaEvaluator evaluator, List<ParsedImportRow> parsedRows, List<String> errors) {
        Cell dateCell = row.getCell(header.dateColumn(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (dateCell == null || formatter.formatCellValue(dateCell, evaluator).trim().isBlank()) return;

        LocalDate date;
        try {
            date = date(dateCell, formatter, evaluator);
        } catch (RuntimeException ignored) {
            return;
        }

        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        boolean hasContent = false;
        for (Map.Entry<Integer, String> entry : header.amountColumns().entrySet()) {
            Cell cell = row.getCell(entry.getKey(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            BigDecimal amount = numericAmount(cell, evaluator);
            if (amount == null) continue;
            try {
                if (amount.signum() < 0) {
                    hasContent = true;
                    errors.add(errorPrefix(row, entry.getKey()) + "금액은 음수일 수 없습니다.");
                } else if (amount.compareTo(MAX_AMOUNT) > 0) {
                    hasContent = true;
                    errors.add(errorPrefix(row, entry.getKey()) + "금액이 저장 가능한 범위를 초과했습니다.");
                } else if (amount.signum() > 0) {
                    hasContent = true;
                    amounts.put(entry.getValue(), amount.setScale(2, RoundingMode.UNNECESSARY));
                }
            } catch (Exception exception) {
                hasContent = true;
                errors.add(errorPrefix(row, entry.getKey()) + "금액을 숫자로 입력해주세요.");
            }
        }

        String note = null;
        if (header.noteColumn() != null) {
            Cell noteCell = row.getCell(header.noteColumn(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (noteCell != null) {
                note = formatter.formatCellValue(noteCell, evaluator).trim();
                if (!note.isBlank()) hasContent = true;
                else note = null;
            }
        }
        if (!hasContent) return;

        parsedRows.add(new ParsedImportRow(row.getRowNum() + 1, date, Map.copyOf(amounts), note));
    }

    private BigDecimal numericAmount(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() != CellType.FORMULA) return null;
        try {
            CellValue value = evaluator.evaluate(cell);
            return value != null && value.getCellType() == CellType.NUMERIC
                    ? BigDecimal.valueOf(value.getNumberValue()) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalDate date(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) throw new IllegalArgumentException("date is missing");
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (!DateUtil.isValidExcelDate(value)) throw new IllegalArgumentException("invalid excel date");
            return validDate(DateUtil.getLocalDateTime(value).toLocalDate());
        }
        String value = formatter.formatCellValue(cell, evaluator).trim();
        for (DateTimeFormatter dateFormatter : DATE_FORMATTERS) {
            try {
                return validDate(LocalDate.parse(value, dateFormatter));
            } catch (DateTimeParseException ignored) {
                // 다음 형식을 확인한다.
            }
        }
        throw new IllegalArgumentException("invalid date");
    }

    private LocalDate validDate(LocalDate date) {
        if (date.isBefore(MIN_DATE) || date.isAfter(MAX_DATE)) {
            throw new IllegalArgumentException("date is out of range");
        }
        return date;
    }

    private String errorPrefix(Row row, int columnIndex) {
        return (row.getRowNum() + 1) + "행 " + columnName(columnIndex) + "열: ";
    }

    private String columnName(int index) {
        StringBuilder result = new StringBuilder();
        int current = index + 1;
        while (current > 0) {
            int remainder = (current - 1) % 26;
            result.insert(0, (char) ('A' + remainder));
            current = (current - 1) / 26;
        }
        return result.toString();
    }

    private ParsedImportFile invalid(String fileName, String fileHash, String error) {
        return new ParsedImportFile(fileName, fileHash, List.of(), Map.of(), List.of(error));
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private record HeaderDefinition(
            int rowIndex,
            int dateColumn,
            Integer noteColumn,
            Map<Integer, String> amountColumns,
            Map<String, String> displayHeaders
    ) {
    }
}
