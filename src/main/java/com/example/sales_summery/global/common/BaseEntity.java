package com.example.sales_summery.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    // 생성·수정 시각 계산에 사용하는 한국 시간대
    protected static final ZoneId KOREA_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 최초 저장 직전에 생성·수정 시각을 함께 설정
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(KOREA_ZONE_ID);
        this.createdAt = now;
        this.updatedAt = now;
    }

    // 수정 직전에 수정 시각만 갱신
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(KOREA_ZONE_ID);
    }

}
