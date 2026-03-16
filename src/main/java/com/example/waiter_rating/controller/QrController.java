package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.response.QrCreateResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.QrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrService qrService;
    private final AuthService authService;
    private final AppUserRepo appUserRepo;

    public QrController(QrService qrService, AuthService authService, AppUserRepo appUserRepo) {
        this.qrService = qrService;
        this.authService = authService;
        this.appUserRepo = appUserRepo;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateQr(
            @RequestParam(required = false) Long businessId,
            @RequestParam(defaultValue = "3") int ttlMinutes) {

        AppUser professional = authService.getCurrentProfessional()
                .orElseThrow(() -> new IllegalStateException("Solo los professionals pueden generar QRs"));

        System.out.println(">>> Generando QR para professional=" + professional.getId() +
                ", businessId=" + businessId + ", ttl=" + ttlMinutes);

        QrCreateResponse resp = qrService.createDynamic(professional.getId(), businessId, ttlMinutes);

        System.out.println(">>> QR generado OK, code=" + resp.getCode());

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> resolve(@PathVariable String code) {
        Long professionalId = qrService.resolveProfessional(code);
        AppUser professional = appUserRepo.findById(professionalId)
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        return ResponseEntity.ok(Map.of(
                "professionalId", professionalId,
                "professionalName", professional.getName(),
                "message", "QR válido"
        ));
    }

    @GetMapping("/resolve/{code}")
    public ResponseEntity<?> resolveAlternative(@PathVariable String code) {
        Long professionalId = qrService.resolveProfessional(code);
        AppUser professional = appUserRepo.findById(professionalId)
                .orElseThrow(() -> new RuntimeException("Profesional no encontrado"));
        return ResponseEntity.ok(Map.of(
                "professionalId", professionalId,
                "professionalName", professional.getName()
        ));
    }

    @PostMapping("/{code}/invalidate")
    public ResponseEntity<?> invalidate(@PathVariable String code) {
        AppUser professional = authService.getCurrentProfessional()
                .orElseThrow(() -> new IllegalStateException("Solo los professionals pueden invalidar QRs"));

        qrService.invalidate(code);
        return ResponseEntity.noContent().build();
    }
}