package com.example.sales_summery.dailynote.dto;

import com.example.sales_summery.dailynote.domain.DailyNote;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyNoteResponse(Long noteId, LocalDate noteDate, String content,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static DailyNoteResponse from(DailyNote note) {
        return new DailyNoteResponse(note.getNoteId(), note.getNoteDate(), note.getContent(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
