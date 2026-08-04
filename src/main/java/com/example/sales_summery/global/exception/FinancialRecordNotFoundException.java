package com.example.sales_summery.global.exception;

// 수입·지출 기록 조회 실패를 표현하는 도메인 예외다.
public class FinancialRecordNotFoundException extends BusinessException {
    public FinancialRecordNotFoundException() {
        super(ErrorCode.RECORD_NOT_FOUND);
    }
}
