package com.finance.app.service;

import com.finance.app.dto.CategoryRequestDTO;
import com.finance.app.dto.CategoryResponseDTO;
import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import com.finance.app.exception.ResourceNotFoundException;
import com.finance.app.repository.CategoryRepository;
import com.finance.app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getAllCategories(CategoryType type) {
        List<Category> categories = (type != null) ? categoryRepository.findByType(type) : categoryRepository.findAll();
        return categories.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO getCategoryById(Long id) {
        Category category = findCategoryEntityById(id);
        return mapToResponseDTO(category);
    }

    @Transactional(readOnly = true)
    public Category findCategoryEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists.");
        }

        Category category = Category.builder()
                .name(dto.getName())
                .type(dto.getType())
                .description(dto.getDescription())
                .icon(dto.getIcon())
                .color(dto.getColor())
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponseDTO(saved);
    }

    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO dto) {
        Category category = findCategoryEntityById(id);

        if (!category.getName().equalsIgnoreCase(dto.getName()) && categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists.");
        }

        if (category.getType() != dto.getType() && transactionRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Cannot change category type for a category with existing historical transactions.");
        }

        category.setName(dto.getName());
        category.setType(dto.getType());
        category.setDescription(dto.getDescription());
        category.setIcon(dto.getIcon());
        category.setColor(dto.getColor());

        Category updated = categoryRepository.save(category);
        return mapToResponseDTO(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryEntityById(id);
        categoryRepository.delete(category);
    }

    public CategoryResponseDTO mapToResponseDTO(Category category) {
        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .description(category.getDescription())
                .icon(category.getIcon())
                .color(category.getColor())
                .build();
    }
}
