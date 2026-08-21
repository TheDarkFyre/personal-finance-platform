package com.finance.app.repository;

import com.finance.app.entity.Category;
import com.finance.app.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findByType(CategoryType type);
    boolean existsByNameIgnoreCase(String name);
}
