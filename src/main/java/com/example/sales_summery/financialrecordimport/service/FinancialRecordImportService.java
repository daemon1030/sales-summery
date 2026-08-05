package com.example.sales_summery.financialrecordimport.service;

import com.example.sales_summery.category.domain.CostType;
import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.category.domain.TransactionType;
import com.example.sales_summery.category.repository.FinancialCategoryRepository;
import com.example.sales_summery.dailynote.domain.DailyNote;
import com.example.sales_summery.dailynote.repository.DailyNoteRepository;
import com.example.sales_summery.financialrecord.domain.FinancialRecord;
import com.example.sales_summery.financialrecord.repository.FinancialRecordRepository;
import com.example.sales_summery.financialrecordimport.domain.FinancialRecordImport;
import com.example.sales_summery.financialrecordimport.domain.FinancialImportColumnMapping;
import com.example.sales_summery.financialrecordimport.dto.ConfirmImportRequest;
import com.example.sales_summery.financialrecordimport.dto.ImportAnalysisResponse;
import com.example.sales_summery.financialrecordimport.dto.ImportColumnAnalysisResponse;
import com.example.sales_summery.financialrecordimport.dto.ImportColumnMappingRequest;
import com.example.sales_summery.financialrecordimport.dto.ImportColumnStatus;
import com.example.sales_summery.financialrecordimport.dto.ImportFileAnalysisResponse;
import com.example.sales_summery.financialrecordimport.dto.ImportFileStatus;
import com.example.sales_summery.financialrecordimport.dto.ImportMappingAction;
import com.example.sales_summery.financialrecordimport.dto.ImportResultResponse;
import com.example.sales_summery.financialrecordimport.repository.FinancialRecordImportRepository;
import com.example.sales_summery.financialrecordimport.repository.FinancialImportColumnMappingRepository;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.CategoryNotFoundException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.UserNotFoundException;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FinancialRecordImportService {
    private static final int MAX_FILES = 50;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final long MAX_TOTAL_SIZE = 100L * 1024 * 1024;

    private final ExcelImportParser parser;
    private final ImportSessionStore sessionStore;
    private final FinancialCategoryRepository categoryRepository;
    private final FinancialRecordRepository recordRepository;
    private final DailyNoteRepository noteRepository;
    private final FinancialRecordImportRepository importRepository;
    private final FinancialImportColumnMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public FinancialRecordImportService(ExcelImportParser parser,
                                        ImportSessionStore sessionStore,
                                        FinancialCategoryRepository categoryRepository,
                                        FinancialRecordRepository recordRepository,
                                        DailyNoteRepository noteRepository,
                                        FinancialRecordImportRepository importRepository,
                                        FinancialImportColumnMappingRepository mappingRepository,
                                        UserRepository userRepository,
                                        Clock clock) {
        this.parser = parser;
        this.sessionStore = sessionStore;
        this.categoryRepository = categoryRepository;
        this.recordRepository = recordRepository;
        this.noteRepository = noteRepository;
        this.importRepository = importRepository;
        this.mappingRepository = mappingRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ImportAnalysisResponse analyze(Long userId, List<MultipartFile> files) {
        validateFiles(files);
        List<FinancialCategory> categories = categoryRepository.findAllByUserUserIdOrderByCategoryNameAsc(userId);
        Map<String, FinancialCategory> activeCategories = new HashMap<>();
        for (FinancialCategory category : categories) {
            if (category.isActive()) {
                activeCategories.putIfAbsent(ExcelImportParser.normalizeHeader(category.getCategoryName()), category);
            }
        }
        for (FinancialImportColumnMapping mapping : mappingRepository.findAllByUserUserId(userId)) {
            if (mapping.getCategory().isActive()) {
                activeCategories.put(mapping.getNormalizedHeader(), mapping.getCategory());
            }
        }

        List<ImportFileAnalysisResponse> fileResponses = new ArrayList<>();
        List<ParsedImportFile> readyFiles = new ArrayList<>();
        Set<String> hashesInRequest = new HashSet<>();
        int duplicateCount = 0;
        int invalidCount = 0;

        for (MultipartFile file : files) {
            String fileName = safeFileName(file.getOriginalFilename());
            try {
                byte[] bytes = file.getBytes();
                String hash = sha256(bytes);
                boolean duplicate = !hashesInRequest.add(hash)
                        || importRepository.existsByUserUserIdAndFileHash(userId, hash);
                if (duplicate) {
                    duplicateCount++;
                    fileResponses.add(new ImportFileAnalysisResponse(fileName, ImportFileStatus.DUPLICATE,
                            0, 0, 0, List.of("이미 가져온 파일과 내용이 같습니다.")));
                    continue;
                }
                ParsedImportFile parsed = parser.parse(fileName, hash, bytes);
                if (!parsed.errors().isEmpty()) {
                    invalidCount++;
                    fileResponses.add(new ImportFileAnalysisResponse(fileName, ImportFileStatus.INVALID,
                            parsed.rows().size(), parsed.recordCount(), parsed.noteCount(), parsed.errors()));
                    continue;
                }
                if (parsed.rows().isEmpty()) {
                    invalidCount++;
                    fileResponses.add(new ImportFileAnalysisResponse(fileName, ImportFileStatus.INVALID,
                            0, 0, 0, List.of("등록할 날짜별 데이터가 없습니다.")));
                    continue;
                }
                readyFiles.add(parsed);
                fileResponses.add(new ImportFileAnalysisResponse(fileName, ImportFileStatus.READY,
                        parsed.rows().size(), parsed.recordCount(), parsed.noteCount(), List.of()));
            } catch (Exception exception) {
                invalidCount++;
                fileResponses.add(new ImportFileAnalysisResponse(fileName, ImportFileStatus.INVALID,
                        0, 0, 0, List.of("파일을 읽을 수 없습니다.")));
            }
        }

        Map<String, ColumnAggregate> aggregates = aggregateColumns(readyFiles);
        Map<String, Long> autoCategoryIds = new LinkedHashMap<>();
        Map<String, String> displayHeaders = new LinkedHashMap<>();
        Set<String> unmappedHeaders = new LinkedHashSet<>();
        List<ImportColumnAnalysisResponse> columnResponses = new ArrayList<>();
        for (Map.Entry<String, ColumnAggregate> entry : aggregates.entrySet()) {
            String normalizedHeader = entry.getKey();
            ColumnAggregate aggregate = entry.getValue();
            displayHeaders.put(normalizedHeader, aggregate.displayHeader);
            FinancialCategory category = activeCategories.get(normalizedHeader);
            if (category != null) {
                autoCategoryIds.put(normalizedHeader, category.getCategoryId());
                columnResponses.add(new ImportColumnAnalysisResponse(aggregate.displayHeader,
                        ImportColumnStatus.AUTO_MATCHED, category.getCategoryId(), category.getCategoryName(),
                        aggregate.valueCount, aggregate.totalAmount));
            } else {
                unmappedHeaders.add(normalizedHeader);
                columnResponses.add(new ImportColumnAnalysisResponse(aggregate.displayHeader,
                        ImportColumnStatus.NEEDS_MAPPING, null, null,
                        aggregate.valueCount, aggregate.totalAmount));
            }
        }

        String token = sessionStore.put(userId, List.copyOf(readyFiles), autoCategoryIds,
                displayHeaders, unmappedHeaders, duplicateCount);
        return new ImportAnalysisResponse(token, List.copyOf(fileResponses), List.copyOf(columnResponses),
                !unmappedHeaders.isEmpty(), readyFiles.size(), duplicateCount, invalidCount);
    }

    @Transactional
    public ImportResultResponse confirm(Long userId, String token, ConfirmImportRequest request) {
        ImportSession session = sessionStore.get(token, userId);
        if (session.files().isEmpty()) {
            throw new BusinessException(ErrorCode.IMPORT_FILE_INVALID);
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Map<String, FinancialCategory> categoriesByHeader = resolveAutoCategories(userId, session);
        Map<String, ImportColumnMappingRequest> requestedMappings = new HashMap<>();
        for (ImportColumnMappingRequest mapping : request.mappings()) {
            requestedMappings.put(ExcelImportParser.normalizeHeader(mapping.header()), mapping);
        }

        int createdCategoryCount = 0;
        for (String header : session.unmappedHeaders()) {
            ImportColumnMappingRequest mapping = requestedMappings.get(header);
            if (mapping == null) throw new BusinessException(ErrorCode.IMPORT_MAPPING_REQUIRED);
            if (mapping.action() == ImportMappingAction.IGNORE) continue;
            FinancialCategory category;
            if (mapping.action() == ImportMappingAction.EXISTING) {
                category = findActiveOwnedCategory(userId, mapping.categoryId());
            } else {
                category = createCategory(user, mapping);
                createdCategoryCount++;
            }
            categoriesByHeader.put(header, category);
            rememberMapping(user, header, category);
        }

        List<FinancialRecord> records = new ArrayList<>();
        List<DailyNote> notes = new ArrayList<>();
        Set<LocalDate> noteDates = new HashSet<>();
        int skippedNotes = 0;
        List<FinancialRecordImport> histories = new ArrayList<>();
        LocalDateTime importedAt = LocalDateTime.now(clock);

        for (ParsedImportFile file : session.files()) {
            int fileRecordCount = 0;
            int fileNoteCount = 0;
            for (ParsedImportRow row : file.rows()) {
                for (Map.Entry<String, BigDecimal> amount : row.amounts().entrySet()) {
                    FinancialCategory category = categoriesByHeader.get(amount.getKey());
                    if (category == null) continue;
                    records.add(FinancialRecord.create(category, row.date(), amount.getValue(), null));
                    fileRecordCount++;
                }
                if (row.note() != null && !row.note().isBlank()) {
                    boolean alreadyExists = !noteDates.add(row.date())
                            || noteRepository.existsByUserUserIdAndNoteDate(userId, row.date());
                    if (alreadyExists) {
                        skippedNotes++;
                    } else {
                        notes.add(DailyNote.create(user, row.date(), row.note()));
                        fileNoteCount++;
                    }
                }
            }
            histories.add(FinancialRecordImport.completed(user, file.fileName(), file.fileHash(),
                    fileRecordCount, fileNoteCount, importedAt));
        }

        recordRepository.saveAll(records);
        noteRepository.saveAll(notes);
        importRepository.saveAll(histories);
        sessionStore.remove(token);
        return new ImportResultResponse(session.files().size(), session.duplicateFileCount(), createdCategoryCount,
                records.size(), notes.size(), skippedNotes);
    }

    private Map<String, FinancialCategory> resolveAutoCategories(Long userId, ImportSession session) {
        Map<String, FinancialCategory> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : session.autoCategoryIds().entrySet()) {
            result.put(entry.getKey(), findActiveOwnedCategory(userId, entry.getValue()));
        }
        return result;
    }

    private FinancialCategory findActiveOwnedCategory(Long userId, Long categoryId) {
        if (categoryId == null) throw new CategoryNotFoundException();
        FinancialCategory category = categoryRepository.findByCategoryIdAndUserUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
        if (!category.isActive()) throw new BusinessException(ErrorCode.CATEGORY_INACTIVE);
        return category;
    }

    private FinancialCategory createCategory(User user, ImportColumnMappingRequest mapping) {
        if (mapping.categoryName() == null || mapping.categoryName().isBlank()
                || mapping.transactionType() == null || mapping.costType() == null || mapping.frequency() == null) {
            throw new BusinessException(ErrorCode.IMPORT_MAPPING_REQUIRED);
        }
        String name = mapping.categoryName().trim();
        if (categoryRepository.existsByUserUserIdAndCategoryName(user.getUserId(), name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
        boolean validIncome = mapping.transactionType() == TransactionType.INCOME
                && mapping.costType() == CostType.NONE;
        boolean validExpense = mapping.transactionType() == TransactionType.EXPENSE
                && (mapping.costType() == CostType.FIXED || mapping.costType() == CostType.VARIABLE);
        if (!validIncome && !validExpense) throw new BusinessException(ErrorCode.INVALID_COST_TYPE);
        return categoryRepository.save(FinancialCategory.create(user, name, mapping.transactionType(),
                mapping.costType(), mapping.frequency()));
    }

    private void rememberMapping(User user, String normalizedHeader, FinancialCategory category) {
        FinancialImportColumnMapping mapping = mappingRepository
                .findByUserUserIdAndNormalizedHeader(user.getUserId(), normalizedHeader)
                .orElseGet(() -> FinancialImportColumnMapping.create(user, category, normalizedHeader));
        mapping.changeCategory(category);
        mappingRepository.save(mapping);
    }

    private Map<String, ColumnAggregate> aggregateColumns(List<ParsedImportFile> files) {
        Map<String, ColumnAggregate> result = new LinkedHashMap<>();
        for (ParsedImportFile file : files) {
            for (ParsedImportRow row : file.rows()) {
                for (Map.Entry<String, BigDecimal> amount : row.amounts().entrySet()) {
                    String display = file.displayHeaders().getOrDefault(amount.getKey(), amount.getKey());
                    result.computeIfAbsent(amount.getKey(), ignored -> new ColumnAggregate(display))
                            .add(amount.getValue());
                }
            }
        }
        return result;
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.size() > MAX_FILES) {
            throw new BusinessException(ErrorCode.IMPORT_FILE_INVALID);
        }
        long totalSize = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
                throw new BusinessException(ErrorCode.IMPORT_FILE_INVALID);
            }
            totalSize += file.getSize();
        }
        if (totalSize > MAX_TOTAL_SIZE) throw new BusinessException(ErrorCode.IMPORT_FILE_INVALID);
    }

    private String safeFileName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) return "import.xlsx";
        String normalized = originalFilename.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        return fileName.length() <= 255 ? fileName : fileName.substring(fileName.length() - 255);
    }

    private String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class ColumnAggregate {
        private final String displayHeader;
        private int valueCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;

        private ColumnAggregate(String displayHeader) {
            this.displayHeader = displayHeader;
        }

        private void add(BigDecimal amount) {
            valueCount++;
            totalAmount = totalAmount.add(amount);
        }
    }
}
