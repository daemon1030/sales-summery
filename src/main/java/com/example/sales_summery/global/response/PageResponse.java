package com.example.sales_summery.global.response;

import java.util.List;
import org.springframework.data.domain.Page;

// Spring Page 내부 구현 대신 클라이언트에 필요한 페이지 정보만 안정적으로 노출한다.
public record PageResponse<T>(List<T> content, int page, int size, long totalElements,
                              int totalPages, boolean first, boolean last) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
}
