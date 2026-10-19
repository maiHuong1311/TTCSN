package com.learnflow.backend.catalog;

import com.learnflow.backend.catalog.domain.Category;

public interface CategoryRepository
    extends org.springframework.data.jpa.repository.JpaRepository<Category, Long> {}
