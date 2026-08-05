package com.example.sales_summery.global.exception;

import org.springframework.http.HttpStatus;

// 오류 코드, HTTP 상태, 기본 메시지를 한곳에서 관리해 API 오류 정책을 통일한다.
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청값이 올바르지 않습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "금액은 0보다 커야 합니다."),
    INVALID_COST_TYPE(HttpStatus.BAD_REQUEST, "거래 유형과 비용 유형의 조합이 올바르지 않습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "로그인 아이디 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    USER_SUSPENDED(HttpStatus.FORBIDDEN, "정지된 사용자입니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 사용자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "수입·지출 항목을 찾을 수 없습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "수입·지출 기록을 찾을 수 없습니다."),
    DAILY_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "날짜별 특이사항을 찾을 수 없습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 로그인 아이디입니다."),
    DUPLICATE_CATEGORY_NAME(HttpStatus.CONFLICT, "이미 사용 중인 항목명입니다."),
    DUPLICATE_DAILY_NOTE(HttpStatus.CONFLICT, "해당 날짜의 특이사항이 이미 존재합니다."),
    CATEGORY_INACTIVE(HttpStatus.BAD_REQUEST, "비활성 항목에는 기록을 등록할 수 없습니다."),
    IMPORT_FILE_INVALID(HttpStatus.BAD_REQUEST, "가져올 수 없는 엑셀 파일입니다."),
    IMPORT_MAPPING_REQUIRED(HttpStatus.BAD_REQUEST, "새로운 엑셀 열의 카테고리 설정이 필요합니다."),
    IMPORT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "엑셀 가져오기 정보를 찾을 수 없습니다. 파일을 다시 올려주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
