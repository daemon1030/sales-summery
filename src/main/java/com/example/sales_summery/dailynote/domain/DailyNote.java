package com.example.sales_summery.dailynote.domain;

import com.example.sales_summery.global.common.BaseEntity;
import com.example.sales_summery.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
// 사용자별 날짜 특이사항
@Table(
        name = "daily_notes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_daily_notes_user_date",
                columnNames = {"user_id", "note_date"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyNote extends BaseEntity {

    // 데이터베이스가 생성하는 특이사항 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "note_id")
    private Long noteId;

    // 특이사항을 소유한 사용자
    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_daily_notes_user"))
    private User user;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 빈 내용은 저장하지 않음
    private DailyNote(User user, LocalDate noteDate, String content) {
        validateContent(content);
        this.user = user;
        this.noteDate = noteDate;
        this.content = content;
    }

    // 유효성 검사를 거치는 생성 경로
    public static DailyNote create(User user, LocalDate noteDate, String content) {
        return new DailyNote(user, noteDate, content);
    }

    public void changeContent(String content) {
        validateContent(content);
        this.content = content;
    }

    // 공백만 있는 특이사항을 차단
    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
