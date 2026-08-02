package com.example.sales_summery.dailynote.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateDailyNoteRequest(
        @NotNull(message = "날짜는 필수입니다.") LocalDate noteDate,
        @NotBlank(message = "특이사항 내용은 필수입니다.") String content
) {
}
