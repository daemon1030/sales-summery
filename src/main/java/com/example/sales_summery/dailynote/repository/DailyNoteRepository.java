package com.example.sales_summery.dailynote.repository;

import com.example.sales_summery.dailynote.domain.DailyNote;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long> {

    // 특정 사용자의 특정 날짜 특이사항 조회
    Optional<DailyNote> findByUserUserIdAndNoteDate(Long userId, LocalDate noteDate);

    // 같은 날짜 특이사항의 중복 여부 확인
    boolean existsByUserUserIdAndNoteDate(Long userId, LocalDate noteDate);

    // 기간 내 특이사항을 날짜순으로 조회
    List<DailyNote> findAllByUserUserIdAndNoteDateBetweenOrderByNoteDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
