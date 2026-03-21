package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.response.AdminRatingResponse;
import com.example.waiter_rating.dto.response.AdminStatsResponse;
import com.example.waiter_rating.dto.response.AdminUserResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.BannedWord;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.BannedWordRepository;
import com.example.waiter_rating.service.AppUserService;
import com.example.waiter_rating.service.EmailService;
import com.example.waiter_rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AppUserService userService;
    private final RatingService ratingService;
    private final EmailService emailService;
    private final AppUserRepo appUserRepo;
    private final BannedWordRepository bannedWordRepo;

    private static final Set<String> ALLOWED_ALIASES = Set.of(
        "hola@calificalo.com.ar",
        "soporte@calificalo.com.ar",
        "noresponder@calificalo.com.ar"
    );

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(userService.listAllForAdmin());
    }

    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<Void> toggleSuspend(@PathVariable Long id) {
        userService.toggleSuspend(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ratings")
    public ResponseEntity<List<AdminRatingResponse>> listRatings() {
        return ResponseEntity.ok(ratingService.listAllForAdmin());
    }

    @DeleteMapping("/ratings/{id}")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
        ratingService.deleteByAdmin(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getAdminStats());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteByAdmin(id);
        return ResponseEntity.ok().build();
    }

    // ========== EMAIL ==========

    @PostMapping("/email/individual")
    public ResponseEntity<?> sendIndividual(@RequestBody Map<String, String> body) {
        String toEmail   = body.get("toEmail");
        String toName    = body.getOrDefault("toName", "");
        String subject   = body.get("subject");
        String message   = body.get("message");
        String replyTo   = body.get("replyTo");

        if (toEmail == null || subject == null || message == null || replyTo == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos requeridos"));

        if (!ALLOWED_ALIASES.contains(replyTo))
            return ResponseEntity.badRequest().body(Map.of("error", "Remitente no permitido"));

        emailService.sendAdminEmail(toEmail, toName, subject, message, replyTo);
        return ResponseEntity.ok(Map.of("message", "Email enviado"));
    }

    @PostMapping("/email/broadcast")
    public ResponseEntity<?> sendBroadcast(@RequestBody Map<String, String> body) {
        String targetRole = body.getOrDefault("targetRole", "ALL");
        String subject    = body.get("subject");
        String message    = body.get("message");
        String replyTo    = body.get("replyTo");

        if (subject == null || message == null || replyTo == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Faltan campos requeridos"));

        if (!ALLOWED_ALIASES.contains(replyTo))
            return ResponseEntity.badRequest().body(Map.of("error", "Remitente no permitido"));

        List<AppUser> recipients = switch (targetRole) {
            case "PROFESSIONAL" -> appUserRepo.findActiveByRole(UserRole.PROFESSIONAL);
            case "CLIENT"       -> appUserRepo.findActiveByRole(UserRole.CLIENT);
            default             -> appUserRepo.findAllActive();
        };

        emailService.sendBroadcastEmail(recipients, subject, message, replyTo);
        return ResponseEntity.ok(Map.of("message", "Broadcast iniciado", "recipients", recipients.size()));
    }

    // ========== BANNED WORDS ==========

    @GetMapping("/banned-words")
    public ResponseEntity<List<BannedWord>> listBannedWords() {
        return ResponseEntity.ok(bannedWordRepo.findAll());
    }

    @PostMapping("/banned-words")
    public ResponseEntity<?> addBannedWord(@RequestBody Map<String, String> body) {
        String word = body.get("word");
        if (word == null || word.isBlank() || word.length() > 100)
            return ResponseEntity.badRequest().body(Map.of("error", "Palabra inválida"));
        if (bannedWordRepo.existsByWordIgnoreCase(word.trim()))
            return ResponseEntity.badRequest().body(Map.of("error", "La palabra ya existe"));
        BannedWord saved = bannedWordRepo.save(BannedWord.builder().word(word.trim().toLowerCase()).build());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/banned-words/{id}")
    public ResponseEntity<Void> deleteBannedWord(@PathVariable Long id) {
        if (!bannedWordRepo.existsById(id)) return ResponseEntity.notFound().build();
        bannedWordRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}