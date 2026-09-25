package com.smartdesk.controller;

import com.smartdesk.dto.category.CategoryResponse;
import com.smartdesk.dto.common.ApiResponse;
import com.smartdesk.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }
}
