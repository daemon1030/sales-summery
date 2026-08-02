package com.example.sales_summery.dailynote.service;

import com.example.sales_summery.dailynote.domain.DailyNote;
import com.example.sales_summery.dailynote.dto.CreateDailyNoteRequest;
import com.example.sales_summery.dailynote.dto.DailyNoteResponse;
import com.example.sales_summery.dailynote.dto.UpdateDailyNoteRequest;
import com.example.sales_summery.dailynote.repository.DailyNoteRepository;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.DailyNoteNotFoundException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.UserNotFoundException;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyNoteService {
    private final DailyNoteRepository noteRepository;
    private final UserRepository userRepository;

    public DailyNoteService(DailyNoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public DailyNoteResponse create(Long userId, CreateDailyNoteRequest request) {
        if (noteRepository.existsByUserUserIdAndNoteDate(userId, request.noteDate())) {
            throw new BusinessException(ErrorCode.DUPLICATE_DAILY_NOTE);
        }
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        return DailyNoteResponse.from(noteRepository.save(
                DailyNote.create(user, request.noteDate(), request.content())));
    }

    @Transactional(readOnly = true)
    public DailyNoteResponse getByDate(Long userId, LocalDate date) {
        return DailyNoteResponse.from(noteRepository.findByUserUserIdAndNoteDate(userId, date)
                .orElseThrow(DailyNoteNotFoundException::new));
    }

    @Transactional(readOnly = true)
    public List<DailyNoteResponse> getRange(Long userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        return noteRepository.findAllByUserUserIdAndNoteDateBetweenOrderByNoteDateAsc(
                userId, startDate, endDate).stream().map(DailyNoteResponse::from).toList();
    }

    @Transactional
    public DailyNoteResponse update(Long userId, Long noteId, UpdateDailyNoteRequest request) {
        DailyNote note = findOwnedNote(userId, noteId);
        note.changeContent(request.content());
        return DailyNoteResponse.from(note);
    }

    @Transactional
    public void delete(Long userId, Long noteId) {
        noteRepository.delete(findOwnedNote(userId, noteId));
    }

    private DailyNote findOwnedNote(Long userId, Long noteId) {
        return noteRepository.findByNoteIdAndUserUserId(noteId, userId)
                .orElseThrow(DailyNoteNotFoundException::new);
    }
}
