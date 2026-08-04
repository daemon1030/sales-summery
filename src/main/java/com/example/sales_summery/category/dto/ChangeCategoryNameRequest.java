package com.example.sales_summery.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 생성 후에는 분류 규칙을 바꾸지 않고 이름만 변경할 수 있다.
public record ChangeCategoryNameRequest(
        @NotBlank(message = "항목명은 필수입니다.")
        @Size(max = 100, message = "항목명은 100자 이하여야 합니다.") String categoryName
) {
}
