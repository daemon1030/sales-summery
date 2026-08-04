package com.example.sales_summery.dailynote.controller;

import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.dailynote.dto.CreateDailyNoteRequest;
import com.example.sales_summery.dailynote.dto.DailyNoteResponse;
import com.example.sales_summery.dailynote.dto.UpdateDailyNoteRequest;
import com.example.sales_summery.dailynote.service.DailyNoteService;
import com.example.sales_summery.global.response.ApiResponse;
import com.example.sales_summery.global.response.MessageResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/daily-notes")
public class DailyNoteController {
    private final DailyNoteService noteService;

    public DailyNoteController(DailyNoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DailyNoteResponse>> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                                 @Valid @RequestBody CreateDailyNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(noteService.create(user.userId(), request)));
    }

    @GetMapping("/date/{date}")
    public ApiResponse<DailyNoteResponse> getByDate(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(noteService.getByDate(user.userId(), date));
    }

    @GetMapping
    public ApiResponse<List<DailyNoteResponse>> getRange(@AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(noteService.getRange(user.userId(), startDate, endDate));
    }

    @PatchMapping("/{noteId}")
    public ApiResponse<DailyNoteResponse> update(@AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long noteId, @Valid @RequestBody UpdateDailyNoteRequest request) {
        return ApiResponse.success(noteService.update(user.userId(), noteId, request));
    }

    @DeleteMapping("/{noteId}")
    public ApiResponse<MessageResponse> delete(@AuthenticationPrincipal AuthenticatedUser user,
                                               @PathVariable Long noteId) {
        noteService.delete(user.userId(), noteId);
        return ApiResponse.success(new MessageResponse("날짜별 특이사항이 삭제되었습니다."));
    }
}
