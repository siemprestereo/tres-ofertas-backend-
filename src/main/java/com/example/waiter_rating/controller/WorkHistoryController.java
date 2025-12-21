package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.WorkHistoryRequest;
import com.example.waiter_rating.dto.response.WorkHistoryResponse;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.model.WorkHistory;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.WorkHistoryService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/work-history")
public class WorkHistoryController {

    private final WorkHistoryService workHistoryService;
    private final AuthService authService;

    public WorkHistoryController(WorkHistoryService workHistoryService, AuthService authService) {
        this.workHistoryService = workHistoryService;
        this.authService = authService;
    }

    // ========== ENDPOINTS PARA EL PROFESSIONAL LOGUEADO (/me) ==========

    /**
     * Listar MI historial laboral
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyWorkHistory(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden acceder a su historial"));
        }

        List<WorkHistoryResponse> history = workHistoryService.listWorkHistory(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(history);
    }

    /**
     * Agregar trabajo a MI historial
     */
    @PostMapping("/me")
    public ResponseEntity<?> addMyWorkHistory(
            @Valid @RequestBody WorkHistoryRequest req,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden agregar historial"));
        }

        WorkHistory wh = workHistoryService.addWorkHistory(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(wh));
    }

    /**
     * Actualizar MI trabajo
     */
    @PutMapping("/me/{workHistoryId}")
    public ResponseEntity<?> updateMyWorkHistory(
            @PathVariable Long workHistoryId,
            @Valid @RequestBody WorkHistoryRequest req,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden editar historial"));
        }

        WorkHistory wh = workHistoryService.updateWorkHistory(userId, workHistoryId, req);
        return ResponseEntity.ok(toResponse(wh));
    }

    /**
     * Cerrar MI trabajo (marcar como finalizado)
     */
    @PostMapping("/me/{workHistoryId}/close")
    public ResponseEntity<?> closeMyWorkHistory(
            @PathVariable Long workHistoryId,
            @RequestBody Map<String, String> body,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden cerrar trabajos"));
        }

        LocalDate endDate = LocalDate.parse(body.get("endDate"), DateTimeFormatter.ISO_LOCAL_DATE);
        WorkHistory wh = workHistoryService.closeWorkHistory(userId, workHistoryId, endDate);
        return ResponseEntity.ok(toResponse(wh));
    }

    /**
     * Eliminar MI trabajo del historial
     */
    @DeleteMapping("/me/{workHistoryId}")
    public ResponseEntity<?> deleteMyWorkHistory(
            @PathVariable Long workHistoryId,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden eliminar historial"));
        }

        workHistoryService.deleteWorkHistory(userId, workHistoryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Habilitar trabajo freelance
     */
    @PostMapping("/me/enable-freelance")
    public ResponseEntity<?> enableFreelance(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String userType = (String) session.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden habilitar freelance"));
        }

        WorkHistory wh = workHistoryService.enableFreelanceWork(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(wh));
    }

    // ========== ENDPOINTS PÚBLICOS ==========

    /**
     * Ver historial laboral de un professional (PÚBLICO)
     */
    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<List<WorkHistoryResponse>> getPublic(@PathVariable Long professionalId) {
        List<WorkHistoryResponse> history = workHistoryService.listWorkHistory(professionalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    /**
     * Ver trabajos activos de un professional (PÚBLICO)
     */
    @GetMapping("/professional/{professionalId}/active")
    public ResponseEntity<List<WorkHistoryResponse>> getActivePublic(@PathVariable Long professionalId) {
        List<WorkHistoryResponse> history = workHistoryService.listActiveWorkHistory(professionalId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(history);
    }

    // ========== MAPPER ==========

    private WorkHistoryResponse toResponse(WorkHistory wh) {
        DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE;

        WorkHistoryResponse dto = new WorkHistoryResponse();
        dto.setId(wh.getId());
        dto.setBusinessId(wh.getBusiness() != null ? wh.getBusiness().getId() : null);
        dto.setBusinessName(wh.getBusiness() != null ? wh.getBusiness().getName() : "N/A");
        dto.setBusinessType(wh.getBusiness() != null ? wh.getBusiness().getBusinessType() : null);
        dto.setPosition(wh.getPosition());
        dto.setStartDate(wh.getStartDate() != null ? wh.getStartDate().format(F) : null);
        dto.setEndDate(wh.getEndDate() != null ? wh.getEndDate().format(F) : null);
        dto.setIsActive(wh.getIsActive());
        dto.setReferenceContact(wh.getReferenceContact());
        return dto;
    }
}