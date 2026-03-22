package com.example.waiter_rating.controller;

import com.example.waiter_rating.model.Profession;
import com.example.waiter_rating.repository.ProfessionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfessionController {

    private final ProfessionRepo professionRepo;

    // Public endpoint for profession selector
    @GetMapping("/api/professions")
    public ResponseEntity<List<Profession>> listActive() {
        return ResponseEntity.ok(professionRepo.findByActiveTrueOrderByDisplayNameAsc());
    }

    // Admin endpoints
    @GetMapping("/api/admin/professions")
    public ResponseEntity<List<Profession>> listAll() {
        return ResponseEntity.ok(professionRepo.findAllByOrderByDisplayNameAsc());
    }

    @PostMapping("/api/admin/professions")
    public ResponseEntity<?> addProfession(@RequestBody Map<String, String> body) {
        String displayName = body.get("displayName");
        if (displayName == null || displayName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El nombre es obligatorio"));
        }

        // Generate code from display name: uppercase, spaces to underscore, remove accents
        String code = displayName.trim().toUpperCase()
                .replaceAll("[áàäâ]", "A").replaceAll("[éèëê]", "E")
                .replaceAll("[íìïî]", "I").replaceAll("[óòöô]", "O")
                .replaceAll("[úùüû]", "U").replaceAll("[ñ]", "N")
                .replaceAll("[^A-Z0-9]", "_").replaceAll("_+", "_")
                .replaceAll("^_|_$", "");

        // Ensure uniqueness
        String finalCode = code;
        int suffix = 1;
        while (professionRepo.existsByCode(finalCode)) {
            finalCode = code + "_" + suffix++;
        }

        Profession profession = new Profession();
        profession.setCode(finalCode);
        profession.setDisplayName(displayName.trim());
        profession.setActive(true);
        return ResponseEntity.ok(professionRepo.save(profession));
    }

    @PatchMapping("/api/admin/professions/{id}/toggle")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        return professionRepo.findById(id).map(p -> {
            p.setActive(!p.isActive());
            return ResponseEntity.ok(professionRepo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }
}
