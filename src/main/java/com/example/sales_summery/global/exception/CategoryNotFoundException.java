package com.example.sales_summery.global.exception;

// 다른 사용자의 항목도 찾을 수 없는 것처럼 처리할 때 함께 사용한다.
public class CategoryNotFoundException extends BusinessException {
    public CategoryNotFoundException() {
        super(ErrorCode.CATEGORY_NOT_FOUND);
    }
}
