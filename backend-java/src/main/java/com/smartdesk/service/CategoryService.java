package com.smartdesk.service;

import com.smartdesk.dto.category.CategoryResponse;
import com.smartdesk.entity.Category;
import com.smartdesk.exception.ResourceNotFoundException;
import com.smartdesk.mapper.EntityDtoMapper;
import com.smartdesk.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue()
                .stream()
                .map(EntityDtoMapper::toCategoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category getCategoryEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }
}
