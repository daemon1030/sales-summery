package com.example.sales_summery.global.exception;

// 날짜별 특이사항 조회 실패를 표현하는 도메인 예외다.
public class DailyNoteNotFoundException extends BusinessException {
    public DailyNoteNotFoundException() {
        super(ErrorCode.DAILY_NOTE_NOT_FOUND);
    }
}
