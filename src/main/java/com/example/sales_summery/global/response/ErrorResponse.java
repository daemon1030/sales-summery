package com.example.sales_summery.global.response;

// 클라이언트가 문자열 메시지가 아닌 안정적인 오류 코드로 분기할 수 있게 함
public record ErrorResponse(String code, String message) {
}
