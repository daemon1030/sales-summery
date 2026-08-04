package com.example.sales_summery.global.exception;

// 사용자 조회 실패를 USER_NOT_FOUND 응답으로 변환하기 위한 도메인 예외다.
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
