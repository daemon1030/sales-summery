package com.example.sales_summery.global.response;

// 수정·삭제처럼 별도 데이터가 필요 없는 성공 응답에 사용한다.
public record MessageResponse(String message) {
}
