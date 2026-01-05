package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.CvDescriptionRequest;
import com.example.waiter_rating.dto.request.WorkHistoryRequest;
import com.example.waiter_rating.dto.response.CvExperienceItem;
import com.example.waiter_rating.dto.response.CvPublicResponse;
import com.example.waiter_rating.model.Cv;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.model.WorkHistory;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.CvService;
import com.example.waiter_rating.service.PdfService;
import com.example.waiter_rating.service.WorkHistoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cv")
public class CvController {

    private final CvService cvService;
    private final WorkHistoryService workHistoryService;
    private final AuthService authService;
    private final PdfService pdfService;

    public CvController(CvService cvService, WorkHistoryService workHistoryService, AuthService authService, PdfService pdfService) {
        this.cvService = cvService;
        this.workHistoryService = workHistoryService;
        this.authService = authService;
        this.pdfService = pdfService;
    }

    // ========== ENDPOINTS PARA EL PROFESSIONAL LOGUEADO (/me) ==========

    /**
     * Obtener MI CV (del professional logueado)
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyCv(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden acceder a su CV"));
        }

        Cv cv = cvService.getOrCreateForProfessional(userId);
        return ResponseEntity.ok(toPublicResponse(cv));
    }

    /**
     * Obtener CV completo para edición (incluye ID del CV y crea si no existe)
     */
    @GetMapping("/me/full")
    public ResponseEntity<?> getMyFullCv(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden acceder a su CV"));
        }

        // Obtener o crear el CV automáticamente
        Cv cv = cvService.getOrCreateForProfessional(userId);

        // Devolver el CV completo con su ID y todas las relaciones
        return ResponseEntity.ok(Map.of(
                "id", cv.getId(),
                "professionalId", cv.getProfessional().getId(),
                "description", cv.getDescription() != null ? cv.getDescription() : "",
                "workExperiences", cv.getProfessional().getWorkHistory() != null
                        ? cv.getProfessional().getWorkHistory().stream().map(this::toItem).toList()
                        : List.of(),
                "education", List.of(), // Placeholder para educación
                "certifications", List.of() // Placeholder para certificaciones
        ));
    }
    /**
     * Actualizar MI descripción
     */
    @PutMapping("/me/description")
    public ResponseEntity<?> updateMyDescription(
            @Valid @RequestBody CvDescriptionRequest req,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden editar su CV"));
        }

        Cv cv = cvService.updateDescription(userId, req.getDescription());
        Cv refreshed = cvService.getPublicCv(userId);

        return ResponseEntity.ok(toPublicResponse(refreshed));
    }

    /**
     * Actualizar CV completo (workExperiences, education, certifications)
     */
    @PutMapping("/{cvId}")
    public ResponseEntity<?> updateFullCv(
            @PathVariable Long cvId,
            @RequestBody Map<String, Object> updates,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden editar su CV"));
        }

        try {
            // Verificar que el CV pertenece al usuario
            Cv cv = cvService.getOrCreateForProfessional(userId);
            if (!cv.getId().equals(cvId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para editar este CV"));
            }

            // TODO: Actualizar workExperiences, education, certifications
            // Por ahora, solo confirmar que funciona
            System.out.println("📝 Actualizando CV " + cvId);
            System.out.println("Work experiences: " + updates.get("workExperiences"));
            System.out.println("Education: " + updates.get("education"));
            System.out.println("Certifications: " + updates.get("certifications"));

            return ResponseEntity.ok(Map.of("message", "CV actualizado correctamente"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar CV: " + e.getMessage()));
        }
    }

    /**
     * Listar MI historial laboral
     */
    @GetMapping("/me/work-history")
    public ResponseEntity<?> listMyWorkHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden ver su historial"));
        }

        List<CvExperienceItem> experiences = workHistoryService.listWorkHistory(userId)
                .stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        return ResponseEntity.ok(experiences);
    }

    /**
     * Agregar trabajo a MI historial
     */
    @PostMapping("/me/work-history")
    public ResponseEntity<?> addMyWorkHistory(
            @Valid @RequestBody WorkHistoryRequest req,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden agregar historial"));
        }

        WorkHistory wh = workHistoryService.addWorkHistory(userId, req);
        return ResponseEntity.ok(toItem(wh));
    }

    /**
     * Actualizar MI trabajo
     */
    @PutMapping("/me/work-history/{workHistoryId}")
    public ResponseEntity<?> updateMyWorkHistory(
            @PathVariable Long workHistoryId,
            @Valid @RequestBody WorkHistoryRequest req,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden editar historial"));
        }

        WorkHistory wh = workHistoryService.updateWorkHistory(userId, workHistoryId, req);
        return ResponseEntity.ok(toItem(wh));
    }

    /**
     * Eliminar MI trabajo del historial
     */
    @DeleteMapping("/me/work-history/{workHistoryId}")
    public ResponseEntity<?> deleteMyWorkHistory(
            @PathVariable Long workHistoryId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

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
    public ResponseEntity<?> enableFreelance(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden habilitar freelance"));
        }

        WorkHistory wh = workHistoryService.enableFreelanceWork(userId);
        return ResponseEntity.ok(toItem(wh));
    }

    // ========== ENDPOINTS PÚBLICOS (por professionalId) ==========

    /**
     * Ver CV público de un professional (PÚBLICO - para empleadores)
     */
    @GetMapping("/professional/{professionalId}")
    public ResponseEntity<CvPublicResponse> getPublic(@PathVariable Long professionalId) {
        Cv cv = cvService.getPublicCv(professionalId);
        return ResponseEntity.ok(toPublicResponse(cv));
    }

    /**
     * Listar historial laboral de un professional (PÚBLICO)
     */
    @GetMapping("/professional/{professionalId}/work-history")
    public ResponseEntity<List<CvExperienceItem>> listWorkHistory(@PathVariable Long professionalId) {
        List<CvExperienceItem> list = workHistoryService.listWorkHistory(professionalId).stream()
                .map(this::toItem)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /**
     * Descargar CV en PDF (PÚBLICO)
     */
    @GetMapping("/{professionalId}/download-pdf")
    public ResponseEntity<byte[]> downloadCvPdf(@PathVariable Long professionalId) {
        try {
            byte[] pdfBytes = pdfService.generateCvPdf(professionalId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("CV_Profesional_" + professionalId + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== MAPPERS ==========

    private CvPublicResponse toPublicResponse(Cv cv) {
        Professional p = cv.getProfessional();

        CvPublicResponse dto = new CvPublicResponse();
        dto.setProfessionalId(p.getId());
        dto.setProfessionalName(p.getName());
        dto.setProfessionalEmail(p.getEmail());
        dto.setProfilePicture(p.getProfilePicture());
        dto.setProfessionType(p.getProfessionType());

        dto.setDescription(cv.getDescription());
        dto.setReputationScore(cv.getReputationScore() != null ? cv.getReputationScore().doubleValue() : 0.0);
        dto.setTotalRatings(cv.getTotalRatings());

        // El historial laboral ahora se obtiene del Professional, no del CV
        dto.setWorkHistory(p.getWorkHistory() != null
                ? p.getWorkHistory().stream().map(this::toItem).toList()
                : List.of());

        return dto;
    }

    private CvExperienceItem toItem(WorkHistory wh) {
        DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE;

        CvExperienceItem item = new CvExperienceItem();
        item.setWorkHistoryId(wh.getId());

        // Priorizar businessName (texto libre)
        String businessName = wh.getBusinessName();
        if (businessName == null || businessName.isEmpty()) {
            businessName = wh.getBusiness() != null ? wh.getBusiness().getName() : "Sin especificar";
        }
        item.setBusinessName(businessName);

        item.setBusinessType(wh.getBusiness() != null ? wh.getBusiness().getBusinessType() : null);
        item.setPosition(wh.getPosition());
        item.setDescription(wh.getDescription());
        item.setStartDate(wh.getStartDate() != null ? wh.getStartDate().format(F) : null);
        item.setEndDate(wh.getEndDate() != null ? wh.getEndDate().format(F) : null);
        item.setIsActive(wh.getIsActive());
        item.setReferenceContact(wh.getReferenceContact());
        return item;
    }


}