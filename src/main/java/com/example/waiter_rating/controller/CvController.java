package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.CvDescriptionRequest;
import com.example.waiter_rating.dto.request.WorkHistoryRequest;
import com.example.waiter_rating.dto.response.CvExperienceItem;
import com.example.waiter_rating.dto.response.CvPublicResponse;
import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
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
    private final EducationService educationService;
    private final CertificationService certificationService;

    private final RatingRepo ratingRepo;

    public CvController(CvService cvService, WorkHistoryService workHistoryService,
                        AuthService authService, PdfService pdfService,
                        EducationService educationService, CertificationService certificationService, RatingRepo ratingRepo) {
        this.cvService = cvService;
        this.workHistoryService = workHistoryService;
        this.authService = authService;
        this.pdfService = pdfService;
        this.educationService = educationService;
        this.certificationService = certificationService;
        this.ratingRepo = ratingRepo;
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

        Cv cv = cvService.getOrCreateForProfessional(userId);

        return ResponseEntity.ok(Map.of(
                "id", cv.getId(),
                "professionalId", cv.getProfessional().getId(),
                "description", cv.getDescription() != null ? cv.getDescription() : "",
                "workExperiences", cv.getProfessional().getWorkHistory() != null
                        ? cv.getProfessional().getWorkHistory().stream().map(this::toItem).toList()
                        : List.of(),
                "education", educationService.getEducationByProfessional(userId),
                "certifications", certificationService.getCertificationsByProfessional(userId)
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

            System.out.println("📝 Actualizando CV " + cvId + " del usuario " + userId);

            // 0. Actualizar descripción (SOBRE MÍ)
            if (updates.containsKey("description")) {
                String description = (String) updates.get("description");
                cvService.updateDescription(userId, description);
                System.out.println("✏️ Descripción actualizada: " + description);
            }

            // 1. Actualizar work experiences
            List<Map<String, Object>> workExperiences = (List<Map<String, Object>>) updates.get("workExperiences");
            if (workExperiences != null) {
                System.out.println("💼 Procesando " + workExperiences.size() + " experiencias laborales");

                for (Map<String, Object> exp : workExperiences) {
                    WorkHistoryRequest req = new WorkHistoryRequest();
                    req.setBusinessName((String) exp.get("company"));
                    req.setPosition((String) exp.get("position"));

                    // Parsear fechas de String a LocalDate
                    String startDateStr = (String) exp.get("startDate");
                    String endDateStr = (String) exp.get("endDate");

                    req.setStartDate(startDateStr != null && !startDateStr.isEmpty() ?
                            java.time.LocalDate.parse(startDateStr) : null);
                    req.setEndDate(endDateStr != null && !endDateStr.isEmpty() ?
                            java.time.LocalDate.parse(endDateStr) : null);

                    req.setDescription((String) exp.get("description"));
                    req.setReferenceContact((String) exp.get("referenceName"));
                    req.setIsFreelance((Boolean) exp.getOrDefault("isFreelance", false));

                    Object workHistoryIdObj = exp.get("workHistoryId");
                    if (workHistoryIdObj != null) {
                        Long workHistoryId = ((Number) workHistoryIdObj).longValue();
                        System.out.println("  ✏️ Actualizando work history ID: " + workHistoryId);
                        workHistoryService.updateWorkHistory(userId, workHistoryId, req);
                    } else if (req.getBusinessName() != null && !req.getBusinessName().isEmpty()) {
                        System.out.println("  ➕ Creando nueva work history");
                        workHistoryService.addWorkHistory(userId, req);
                    }
                }
            }

            // 2. Actualizar education
            List<Map<String, Object>> educationList = (List<Map<String, Object>>) updates.get("education");
            if (educationList != null) {
                System.out.println("🎓 Procesando " + educationList.size() + " registros de educación");

                // Primero eliminar todos los existentes
                educationService.deleteAllByProfessional(userId);

                // Crear los nuevos
                for (Map<String, Object> eduMap : educationList) {
                    if (eduMap.get("institution") != null && !eduMap.get("institution").toString().isEmpty()) {
                        Education edu = new Education();
                        edu.setInstitution((String) eduMap.get("institution"));
                        edu.setDegree((String) eduMap.get("degree"));
                        edu.setStartDate(eduMap.get("startDate") != null ?
                                java.time.LocalDate.parse((String) eduMap.get("startDate")) : null);
                        edu.setEndDate(eduMap.get("endDate") != null ?
                                java.time.LocalDate.parse((String) eduMap.get("endDate")) : null);
                        edu.setCurrentlyStudying((Boolean) eduMap.getOrDefault("currentlyStudying", false));
                        edu.setDescription((String) eduMap.get("description"));

                        System.out.println("  ➕ Creando educación: " + edu.getInstitution());
                        educationService.addEducation(userId, edu);
                    }
                }
            }

            // 3. Actualizar certifications
            List<Map<String, Object>> certificationsList = (List<Map<String, Object>>) updates.get("certifications");
            if (certificationsList != null) {
                System.out.println("🏆 Procesando " + certificationsList.size() + " certificaciones");

                // Primero eliminar todos los existentes
                certificationService.deleteAllByProfessional(userId);

                // Crear los nuevos
                for (Map<String, Object> certMap : certificationsList) {
                    if (certMap.get("name") != null && !certMap.get("name").toString().isEmpty()) {
                        Certification cert = new Certification();
                        cert.setName((String) certMap.get("name"));
                        cert.setIssuer((String) certMap.get("issuer"));
                        cert.setDateObtained(certMap.get("dateObtained") != null ?
                                java.time.LocalDate.parse((String) certMap.get("dateObtained")) : null);
                        cert.setExpiryDate(certMap.get("expiryDate") != null ?
                                java.time.LocalDate.parse((String) certMap.get("expiryDate")) : null);

                        System.out.println("  ➕ Creando certificación: " + cert.getName());
                        certificationService.addCertification(userId, cert);
                    }
                }
            }

            System.out.println("✅ CV actualizado correctamente");
            return ResponseEntity.ok(Map.of("message", "CV actualizado correctamente"));

        } catch (Exception e) {
            System.err.println("❌ Error actualizando CV: " + e.getMessage());
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
        dto.setProfessionalPhone(p.getPhone());
        dto.setProfessionalLocation(p.getLocation());
        dto.setProfilePicture(p.getProfilePicture());
        dto.setProfessionType(p.getProfessionType());

        dto.setDescription(cv.getDescription());
        dto.setReputationScore(cv.getReputationScore() != null ? cv.getReputationScore().doubleValue() : 0.0);
        dto.setTotalRatings(cv.getTotalRatings());

        // Historial laboral
        dto.setWorkHistory(p.getWorkHistory() != null
                ? p.getWorkHistory().stream().map(this::toItem).toList()
                : List.of());

        // Education y Certifications
        dto.setEducation(educationService.getEducationByProfessional(p.getId()));
        dto.setCertifications(certificationService.getCertificationsByProfessional(p.getId()));

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
        item.setIsFreelance(wh.getIsFreelance() != null ? wh.getIsFreelance() : false);
        item.setReferenceContact(wh.getReferenceContact());

        // ✅ AGREGAR ESTO:
        long ratingCount = ratingRepo.countByWorkHistoryId(wh.getId());
        item.setTotalRatings((int) ratingCount);

        return item;
    }
}