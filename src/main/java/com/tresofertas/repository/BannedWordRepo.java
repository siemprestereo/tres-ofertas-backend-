package com.tresofertas.repository;

import com.tresofertas.model.BannedWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannedWordRepo extends JpaRepository<BannedWord, Long> {
    boolean existsByWordIgnoreCase(String word);
}
