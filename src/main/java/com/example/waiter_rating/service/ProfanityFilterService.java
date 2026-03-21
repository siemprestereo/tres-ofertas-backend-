package com.example.waiter_rating.service;

import com.example.waiter_rating.repository.BannedWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfanityFilterService {

    private final BannedWordRepository bannedWordRepo;

    /**
     * Returns true if the text contains any banned word.
     * Matching is case-insensitive and checks for substring occurrence.
     */
    public boolean containsProfanity(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return bannedWordRepo.findAll().stream()
                .anyMatch(bw -> lower.contains(bw.getWord().toLowerCase()));
    }
}
