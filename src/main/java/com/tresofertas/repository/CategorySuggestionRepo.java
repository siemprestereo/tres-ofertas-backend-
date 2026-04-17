package com.tresofertas.repository;

import com.tresofertas.model.CategorySuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategorySuggestionRepo extends JpaRepository<CategorySuggestion, Long> {
    List<CategorySuggestion> findByReviewedFalse();
}
