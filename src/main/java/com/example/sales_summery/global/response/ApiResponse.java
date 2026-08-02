package com.example.sales_summery.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;

// 모든 API가 동일한 성공·실패 JSON 구조를 사용하도록 감싸는 공통 응답
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {

    // 성공 응답에서는 data만 노출하고 error 필드는 JSON에서 제외
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 실패 응답에서는 오류 정보만 노출하고 data 필드는 JSON에서 제외
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }
}
