package com.example.waiter_rating.controller;

import com.example.waiter_rating.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/suggestions")
@RequiredArgsConstructor
@Slf4j
public class SuggestionController {

    private final EmailService emailService;

    @PostMapping("/profession")
    public ResponseEntity<?> suggestProfession(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String suggestion = body.get("suggestion");

        if (suggestion == null || suggestion.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La sugerencia no puede estar vacía"));
        }

        if (suggestion.length() > 100) {
            return ResponseEntity.badRequest().body(Map.of("error", "La sugerencia no puede superar los 100 caracteres"));
        }

        String professionalEmail = userDetails.getUsername();
        String professionalName = body.getOrDefault("professionalName", professionalEmail);

        emailService.sendProfessionSuggestionEmail(professionalName, professionalEmail, suggestion.trim());

        return ResponseEntity.ok(Map.of("message", "Sugerencia enviada. ¡Gracias!"));
    }
}
