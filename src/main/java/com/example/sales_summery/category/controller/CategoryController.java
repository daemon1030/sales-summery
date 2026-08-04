package com.example.sales_summery.category.controller;

import com.example.sales_summery.auth.security.AuthenticatedUser;
import com.example.sales_summery.category.dto.CategoryResponse;
import com.example.sales_summery.category.dto.ChangeCategoryNameRequest;
import com.example.sales_summery.category.dto.CreateCategoryRequest;
import com.example.sales_summery.category.service.CategoryService;
import com.example.sales_summery.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getCategories(@AuthenticationPrincipal AuthenticatedUser user,
                                                             @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.success(categoryService.getCategories(user.userId(), activeOnly));
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategory(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @PathVariable Long categoryId) {
        return ApiResponse.success(categoryService.getCategory(user.userId(), categoryId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                                @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryService.create(user.userId(), request)));
    }

    @PatchMapping("/{categoryId}/name")
    public ApiResponse<CategoryResponse> changeName(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long categoryId,
                                                    @Valid @RequestBody ChangeCategoryNameRequest request) {
        return ApiResponse.success(categoryService.changeName(user.userId(), categoryId, request));
    }

    @PatchMapping("/{categoryId}/activate")
    public ApiResponse<CategoryResponse> activate(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable Long categoryId) {
        return ApiResponse.success(categoryService.activate(user.userId(), categoryId));
    }

    @PatchMapping("/{categoryId}/deactivate")
    public ApiResponse<CategoryResponse> deactivate(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @PathVariable Long categoryId) {
        return ApiResponse.success(categoryService.deactivate(user.userId(), categoryId));
    }
}
