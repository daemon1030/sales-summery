package com.example.sales_summery.dailynote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.sales_summery.auth.dto.SignupRequest;
import com.example.sales_summery.auth.dto.SignupResponse;
import com.example.sales_summery.auth.service.SignupService;
import com.example.sales_summery.dailynote.dto.CreateDailyNoteRequest;
import com.example.sales_summery.dailynote.dto.UpdateDailyNoteRequest;
import com.example.sales_summery.dailynote.service.DailyNoteService;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.DailyNoteNotFoundException;
import com.example.sales_summery.global.exception.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyNoteServiceIntegrationTest {
    @Autowired SignupService signupService;
    @Autowired DailyNoteService noteService;

    @Test
    void noteCanBeCreatedReadUpdatedAndDeleted() {
        SignupResponse user = signup("note_01");
        LocalDate date = LocalDate.of(2026, 8, 2);
        var created = noteService.create(user.userId(), new CreateDailyNoteRequest(date, "비가 많이 옴"));

        assertThat(noteService.getByDate(user.userId(), date).content()).isEqualTo("비가 많이 옴");
        assertThat(noteService.update(user.userId(), created.noteId(),
                new UpdateDailyNoteRequest("휴무일")).content()).isEqualTo("휴무일");
        noteService.delete(user.userId(), created.noteId());
        assertThatThrownBy(() -> noteService.getByDate(user.userId(), date))
                .isInstanceOf(DailyNoteNotFoundException.class);
    }

    @Test
    void duplicateDateAndBlankContentAreRejected() {
        SignupResponse user = signup("note_02");
        LocalDate date = LocalDate.of(2026, 8, 2);
        noteService.create(user.userId(), new CreateDailyNoteRequest(date, "첫 메모"));

        assertThatThrownBy(() -> noteService.create(user.userId(),
                new CreateDailyNoteRequest(date, "두 번째 메모")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_DAILY_NOTE));
        assertThatThrownBy(() -> noteService.create(user.userId(),
                new CreateDailyNoteRequest(date.plusDays(1), "   ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rangeIsDateOrderedAndOtherUserCannotModify() {
        SignupResponse owner = signup("note_03");
        SignupResponse other = signup("note_04");
        var later = noteService.create(owner.userId(), new CreateDailyNoteRequest(
                LocalDate.of(2026, 8, 3), "나중"));
        noteService.create(owner.userId(), new CreateDailyNoteRequest(LocalDate.of(2026, 8, 1), "먼저"));

        assertThat(noteService.getRange(owner.userId(), LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 3))).extracting("content").containsExactly("먼저", "나중");
        assertThatThrownBy(() -> noteService.update(other.userId(), later.noteId(),
                new UpdateDailyNoteRequest("침범"))).isInstanceOf(DailyNoteNotFoundException.class);
    }

    private SignupResponse signup(String loginId) {
        return signupService.signup(new SignupRequest(loginId, "password123", "가게"));
    }
}
