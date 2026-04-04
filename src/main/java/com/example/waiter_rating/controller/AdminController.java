package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.response.AdminRatingResponse;
import com.example.waiter_rating.dto.response.AdminStatsResponse;
import com.example.waiter_rating.dto.response.AdminUserResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.BannedWord;
import com.example.waiter_rating.model.ContactMessage;
import com.example.waiter_rating.model.ContactMessageStatus;
import com.example.waiter_rating.model.Profession;
import com.example.waiter_rating.service.ProfanityFilterService;
import com.example.waiter_rating.model.EmailLog;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.BannedWordRepository;
import com.example.waiter_rating.repository.ContactMessageRepo;
import com.example.waiter_rating.repository.EmailLogRepository;
import com.example.waiter_rating.repository.ProfessionRepo;
import com.example.waiter_rating.repository.RatingRepo;
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
    private final EmailLogRepository emailLogRepo;
    private final ProfanityFilterService profanityFilter;
    private final com.example.waiter_rating.repository.RatingRepo ratingRepo;
    private final ContactMessageRepo contactMessageRepo;
    private final ProfessionRepo professionRepo;

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

    @PatchMapping("/ratings/{id}/clear-comment")
    public ResponseEntity<Void> clearRatingComment(@PathVariable Long id) {
        ratingService.clearCommentByAdmin(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getAdminStats());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteByAdmin(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage(), "cause", e.getCause() != null ? e.getCause().getMessage() : ""));
        }
    }

    @PatchMapping("/users/{id}/verify-email")
    public ResponseEntity<?> verifyEmail(@PathVariable Long id) {
        userService.verifyEmailByAdmin(id);
        return ResponseEntity.ok(Map.of("message", "Email verificado correctamente"));
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

        emailLogRepo.save(EmailLog.builder()
                .type("INDIVIDUAL")
                .subject(subject)
                .recipientEmail(toEmail)
                .recipientName(toName)
                .senderAlias(replyTo)
                .bodyPreview(message.length() > 300 ? message.substring(0, 297) + "..." : message)
                .recipientsCount(1)
                .build());

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

        emailLogRepo.save(EmailLog.builder()
                .type("BROADCAST")
                .subject(subject)
                .targetRole(targetRole)
                .senderAlias(replyTo)
                .bodyPreview(message.length() > 300 ? message.substring(0, 297) + "..." : message)
                .recipientsCount(recipients.size())
                .build());

        return ResponseEntity.ok(Map.of("message", "Broadcast iniciado", "recipients", recipients.size()));
    }

    @GetMapping("/email/log")
    public ResponseEntity<List<EmailLog>> getEmailLog() {
        return ResponseEntity.ok(emailLogRepo.findTop100ByOrderBySentAtDesc());
    }

    @DeleteMapping("/email/log/{id}")
    public ResponseEntity<Void> deleteEmailLog(@PathVariable Long id) {
        emailLogRepo.deleteById(id);
        return ResponseEntity.noContent().build();
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
        profanityFilter.evictCache();
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/banned-words/{id}")
    public ResponseEntity<Void> deleteBannedWord(@PathVariable Long id) {
        if (!bannedWordRepo.existsById(id)) return ResponseEntity.notFound().build();
        bannedWordRepo.deleteById(id);
        profanityFilter.evictCache();
        return ResponseEntity.noContent().build();
    }

    // ========== STATS ==========

    @GetMapping("/stats/activity")
    public ResponseEntity<?> getActivity() {
        var recentUsers = appUserRepo.findTop5ByOrderByCreatedAtDesc().stream()
                .map(u -> Map.of(
                        "id", u.getId(),
                        "name", u.getName(),
                        "email", u.getEmail(),
                        "activeRole", u.getActiveRole().name(),
                        "createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
                )).toList();

        var recentRatings = ratingRepo.findTop5ByOrderByCreatedAtDesc().stream()
                .map(r -> new AdminRatingResponse(
                        r.getId(),
                        r.getClient() != null ? r.getClient().getName() : "Cliente eliminado",
                        r.getProfessional() != null ? r.getProfessional().getName() : r.getProfessionalName(),
                        r.getScore(), r.getComment(), r.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(Map.of("recentUsers", recentUsers, "recentRatings", recentRatings));
    }

    @GetMapping("/stats/trends")
    public ResponseEntity<?> getTrends() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime since = now.minusWeeks(8);

        List<com.example.waiter_rating.model.AppUser> users = appUserRepo.findByCreatedAtAfter(since);
        List<com.example.waiter_rating.model.Rating> ratings = ratingRepo.findByCreatedAtAfter(since);

        var points = new java.util.ArrayList<Map<String, Object>>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM", new java.util.Locale("es", "AR"));

        for (int i = 7; i >= 0; i--) {
            java.time.LocalDateTime weekStart = now.minusWeeks(i + 1).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
            java.time.LocalDateTime weekEnd   = now.minusWeeks(i).truncatedTo(java.time.temporal.ChronoUnit.DAYS);

            long regCount = users.stream()
                    .filter(u -> u.getCreatedAt() != null
                            && !u.getCreatedAt().isBefore(weekStart)
                            && u.getCreatedAt().isBefore(weekEnd))
                    .count();
            long ratingCount = ratings.stream()
                    .filter(r -> r.getCreatedAt() != null
                            && !r.getCreatedAt().isBefore(weekStart)
                            && r.getCreatedAt().isBefore(weekEnd))
                    .count();

            points.add(Map.of("week", weekStart.format(fmt), "registrations", regCount, "ratings", ratingCount));
        }

        return ResponseEntity.ok(points);
    }

    @GetMapping("/users/{id}/ratings")
    public ResponseEntity<List<AdminRatingResponse>> getUserRatings(@PathVariable Long id) {
        AppUser user = appUserRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return ResponseEntity.ok(ratingService.getUserRatingsForAdmin(id, user.getActiveRole().name()));
    }

    // ========== CONTACT MESSAGES ==========

    @GetMapping("/messages")
    public ResponseEntity<List<ContactMessage>> listMessages() {
        return ResponseEntity.ok(contactMessageRepo.findAllByOrderByCreatedAtDesc());
    }

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<ContactMessage> markRead(@PathVariable Long id) {
        ContactMessage cm = contactMessageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));
        cm.setRead(true);
        return ResponseEntity.ok(contactMessageRepo.save(cm));
    }

    @PatchMapping("/messages/{id}/resolve")
    public ResponseEntity<ContactMessage> resolveMessage(@PathVariable Long id) {
        ContactMessage cm = contactMessageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));
        cm.setStatus(ContactMessageStatus.RESOLVED);
        return ResponseEntity.ok(contactMessageRepo.save(cm));
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        contactMessageRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/messages/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", contactMessageRepo.countByReadFalse()));
    }

    @PostMapping("/messages/{id}/accept-suggestion")
    public ResponseEntity<?> acceptSuggestion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ContactMessage cm = contactMessageRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mensaje no encontrado"));

        String displayName = body.get("displayName");
        if (displayName == null || displayName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
        }

        // Generate code
        String code = displayName.trim().toUpperCase()
                .replaceAll("[áàäâÁÀÄÂ]", "A").replaceAll("[éèëêÉÈËÊ]", "E")
                .replaceAll("[íìïîÍÌÏÎ]", "I").replaceAll("[óòöôÓÒÖÔ]", "O")
                .replaceAll("[úùüûÚÙÜÛ]", "U").replaceAll("[ñÑ]", "N")
                .replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        String finalCode = code;
        int suffix = 1;
        while (professionRepo.existsByCode(finalCode)) {
            finalCode = code + "_" + suffix++;
        }

        String category = body.get("category");

        Profession profession = new Profession();
        profession.setCode(finalCode);
        profession.setDisplayName(displayName.trim());
        profession.setCategory(category != null && !category.isBlank() ? category.trim() : null);
        profession.setActive(true);
        professionRepo.save(profession);

        // Send acceptance email
        if (cm.getSenderEmail() != null && !cm.getSenderEmail().equals("desconocido")) {
            emailService.sendSuggestionAcceptedEmail(cm.getSenderEmail(), cm.getSenderName(), displayName.trim());
        }

        // Mark message as resolved
        cm.setRead(true);
        cm.setStatus(ContactMessageStatus.RESOLVED);
        contactMessageRepo.save(cm);

        return ResponseEntity.ok(Map.of("message", "Profesión agregada correctamente", "code", finalCode));
    }

}