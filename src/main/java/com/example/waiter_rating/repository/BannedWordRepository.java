package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.BannedWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannedWordRepository extends JpaRepository<BannedWord, Long> {
    boolean existsByWordIgnoreCase(String word);
}
