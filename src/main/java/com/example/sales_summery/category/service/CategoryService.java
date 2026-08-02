package com.example.sales_summery.category.service;

import com.example.sales_summery.category.domain.FinancialCategory;
import com.example.sales_summery.category.dto.CategoryResponse;
import com.example.sales_summery.category.dto.ChangeCategoryNameRequest;
import com.example.sales_summery.category.dto.CreateCategoryRequest;
import com.example.sales_summery.category.repository.FinancialCategoryRepository;
import com.example.sales_summery.global.exception.BusinessException;
import com.example.sales_summery.global.exception.CategoryNotFoundException;
import com.example.sales_summery.global.exception.ErrorCode;
import com.example.sales_summery.global.exception.UserNotFoundException;
import com.example.sales_summery.user.domain.User;
import com.example.sales_summery.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final FinancialCategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(FinancialCategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories(Long userId, boolean activeOnly) {
        List<FinancialCategory> categories = activeOnly
                ? categoryRepository.findAllByUserUserIdAndActiveTrueOrderByCategoryNameAsc(userId)
                : categoryRepository.findAllByUserUserIdOrderByCategoryNameAsc(userId);
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long userId, Long categoryId) {
        return CategoryResponse.from(findOwnedCategory(userId, categoryId));
    }

    @Transactional
    public CategoryResponse create(Long userId, CreateCategoryRequest request) {
        ensureUniqueName(userId, request.categoryName());
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        FinancialCategory category = FinancialCategory.create(user, request.categoryName(),
                request.transactionType(), request.costType(), request.frequency());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse changeName(Long userId, Long categoryId, ChangeCategoryNameRequest request) {
        FinancialCategory category = findOwnedCategory(userId, categoryId);
        if (!category.getCategoryName().equals(request.categoryName())) {
            ensureUniqueName(userId, request.categoryName());
            category.changeName(request.categoryName());
        }
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse activate(Long userId, Long categoryId) {
        FinancialCategory category = findOwnedCategory(userId, categoryId);
        category.activate();
        return CategoryResponse.from(category);
    }

    @Transactional
    public CategoryResponse deactivate(Long userId, Long categoryId) {
        FinancialCategory category = findOwnedCategory(userId, categoryId);
        category.deactivate();
        return CategoryResponse.from(category);
    }

    private FinancialCategory findOwnedCategory(Long userId, Long categoryId) {
        return categoryRepository.findByCategoryIdAndUserUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);
    }

    private void ensureUniqueName(Long userId, String name) {
        if (categoryRepository.existsByUserUserIdAndCategoryName(userId, name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CATEGORY_NAME);
        }
    }
}
