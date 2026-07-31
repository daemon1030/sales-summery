package com.example.sales_summery.user.domain;

import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.dailynote.domain.DailyNote;
import com.example.sales_summery.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(              // 중복 불가 제약조건
        name = "uk_users_login_id",
        columnNames = "login_id"
    ),
    check = @CheckConstraint(                           // 1~28일 사이로만 제한하는 제약조건
        name = "chk_users_settlement_start_day",
        constraint = "settlement_start_day between 1 and 28"
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)         // 엔터티의 PK값을 JPA가 자동으로 만들어 달라는 코드
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "login_id", nullable = false, length = 10)   // 아이디는 4~10자리
    @Size(min = 4, max = 10)
    @Pattern(regexp = "^[a-z0-9_]+$")
    private String loginId;

    @Column(name = "password_hash", nullable = false, length = 255)
    @Getter(AccessLevel.NONE)
    private String passwordHash;

    @Column(nullable = false, length = 10)
    private String name;

    @Enumerated(EnumType.STRING)                // UserStatus에 있는 값으로만 지정가능
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Min(1)
    @Max(28)
    @Column(name = "settlement_start_day", nullable = false, columnDefinition = "SMALLINT DEFAULT 1")       // 1이 기본값
    private int settlementStartDay;

    @Column(name = "withdrawn_at")                // 사용자가 탈퇴한 시간
    private LocalDateTime withdrawnAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)            // financialCategory.user와 연결, 연쇄 삭제 포함
    @Getter(AccessLevel.NONE)
    private final List<FinancialCategory> financialCategories = new ArrayList<>();      // financualCategory 배열 객체 생성

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)            // dailyNote.user와 연결, 연쇄 삭제
    @Getter(AccessLevel.NONE)
    private final List<DailyNote> dailyNotes = new ArrayList<>();

    private User(String loginId, String passwordHash, String name, int settlementStartDay) {        // User 생성자 (private라 new로 생성하지 못함)
        validateSettlementStartDay(settlementStartDay);
        this.loginId = validateAndNormalizeLoginId(loginId);
        this.passwordHash = passwordHash;
        this.name = name;
        this.status = UserStatus.ACTIVE;
        this.settlementStartDay = settlementStartDay;
    }

    public static User create(String loginId, String passwordHash, String name, int settlementStartDay) {   // create를 통해서만 생성가능
        return new User(loginId, passwordHash, name, settlementStartDay);
    }

    public void changeSettlementStartDay(int settlementStartDay) {      // 기준일을 바꾸는 함수
        validateSettlementStartDay(settlementStartDay);
        this.settlementStartDay = settlementStartDay;
    }

    public void withdraw() {                                            // 탈퇴로 상태를 바꿔줌, 탈퇴한 시간 기록
        this.status = UserStatus.WITHDRAWN;
        this.withdrawnAt = LocalDateTime.now(KOREA_ZONE_ID);
    }

    public List<FinancialCategory> getFinancialCategories() {           // coptOf로 안의 리스트를 수정하지 못하게 가져옴
        return List.copyOf(financialCategories);
    }

    public List<DailyNote> getDailyNotes() {
        return List.copyOf(dailyNotes);
    }

    private static String validateAndNormalizeLoginId(String loginId) {         // ID 소문자, 길이 체크, 정규식 체크하는 함수
        if (loginId == null) {
            throw new IllegalArgumentException("loginId must not be null");
        }

        String normalizedLoginId = loginId.toLowerCase(Locale.ROOT);            // ID를 모두 소문자로 변환
        if (normalizedLoginId.length() < 4 || normalizedLoginId.length() > 10) {
            throw new IllegalArgumentException("loginId length must be between 4 and 10");
        }
        if (!normalizedLoginId.matches("^[a-z0-9_]+$")) {                // 
            throw new IllegalArgumentException("loginId must contain only lowercase letters, numbers, and underscore");
        }

        return normalizedLoginId;
    }

    private static void validateSettlementStartDay(int settlementStartDay) {        // 기준일이 1~28일 사이인지 보고 아닐 시 예외처리        
        if (settlementStartDay < 1 || settlementStartDay > 28) {
            throw new IllegalArgumentException("settlementStartDay must be between 1 and 28");
        }
    }
}
