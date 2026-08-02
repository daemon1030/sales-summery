package com.example.sales_summery.dailynote.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDailyNoteRequest(
        @NotBlank(message = "특이사항 내용은 필수입니다.") String content
) {
}
