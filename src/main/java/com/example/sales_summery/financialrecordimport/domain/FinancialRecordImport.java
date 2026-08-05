package com.example.sales_summery.financialrecordimport.domain;

import com.example.sales_summery.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "financial_record_imports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_financial_record_imports_user_hash",
                columnNames = {"user_id", "file_hash"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FinancialRecordImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "import_id")
    private Long importId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_financial_record_imports_user"))
    private User user;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "record_count", nullable = false)
    private int recordCount;

    @Column(name = "note_count", nullable = false)
    private int noteCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private FinancialRecordImport(User user, String originalFilename, String fileHash,
                                  int recordCount, int noteCount, LocalDateTime createdAt) {
        this.user = user;
        this.originalFilename = originalFilename;
        this.fileHash = fileHash;
        this.recordCount = recordCount;
        this.noteCount = noteCount;
        this.createdAt = createdAt;
    }

    public static FinancialRecordImport completed(User user, String originalFilename, String fileHash,
                                                   int recordCount, int noteCount, LocalDateTime createdAt) {
        return new FinancialRecordImport(user, originalFilename, fileHash, recordCount, noteCount, createdAt);
    }
}
