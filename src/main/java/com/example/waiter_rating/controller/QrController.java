package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.response.QrCreateResponse;
import com.example.waiter_rating.model.Professional;
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

    public QrController(QrService qrService, AuthService authService) {
        this.qrService = qrService;
        this.authService = authService;
    }

    /**
     * Genera un QR dinámico (solo PROFESSIONAL autenticado)
     * Usa el professionalId del usuario logueado automáticamente
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateQr(
            @RequestParam(required = false) Long businessId,
            @RequestParam(defaultValue = "3") int ttlMinutes) {

        // Verificar que el usuario es un Professional
        Professional professional = authService.getCurrentProfessional()
                .orElseThrow(() -> new IllegalStateException("Solo los professionals pueden generar QRs"));

        System.out.println(">>> Generando QR para professional=" + professional.getId() +
                ", businessId=" + businessId + ", ttl=" + ttlMinutes);

        QrCreateResponse resp = qrService.createDynamic(professional.getId(), businessId, ttlMinutes);

        System.out.println(">>> QR generado OK, code=" + resp.getCode());

        return ResponseEntity.ok(resp);
    }

    /**
     * Resuelve el QR: devuelve el professionalId si está vigente
     * (PÚBLICO - para que cualquiera pueda escanear)
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> resolve(@PathVariable String code) {
        Long professionalId = qrService.resolveProfessional(code);
        return ResponseEntity.ok(Map.of(
                "professionalId", professionalId,
                "message", "QR válido"
        ));
    }

    /**
     * Resuelve QR (endpoint alternativo - PÚBLICO)
     */
    @GetMapping("/resolve/{code}")
    public ResponseEntity<?> resolveAlternative(@PathVariable String code) {
        Long professionalId = qrService.resolveProfessional(code);
        return ResponseEntity.ok(Map.of("professionalId", professionalId));
    }

    /**
     * Invalida un QR antes de su expiración (solo el PROFESSIONAL dueño)
     */
    @PostMapping("/{code}/invalidate")
    public ResponseEntity<?> invalidate(@PathVariable String code) {
        // Verificar que es un Professional
        Professional professional = authService.getCurrentProfessional()
                .orElseThrow(() -> new IllegalStateException("Solo los professionals pueden invalidar QRs"));

        // TODO: Verificar que el QR pertenece a este professional

        qrService.invalidate(code);
        return ResponseEntity.noContent().build();
    }
}