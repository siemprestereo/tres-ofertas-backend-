package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.CvDescriptionRequest;
import com.example.waiter_rating.dto.request.PdfGenerationRequest;
import com.example.waiter_rating.dto.request.WorkHistoryRequest;
import com.example.waiter_rating.dto.response.CvExperienceItem;
import com.example.waiter_rating.dto.response.CvPublicResponse;
import com.example.waiter_rating.dto.response.ZoneResponse;
import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.ProfessionalZoneRepo;
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

    private final ProfessionalZoneRepo professionalZoneRepo;

    public CvController(CvService cvService, WorkHistoryService workHistoryService,
                        AuthService authService, PdfService pdfService,
                        EducationService educationService, CertificationService certificationService,
                        RatingRepo ratingRepo, ProfessionalZoneRepo professionalZoneRepo) {
        this.cvService = cvService;
        this.workHistoryService = workHistoryService;
        this.authService = authService;
        this.pdfService = pdfService;
        this.educationService = educationService;
        this.certificationService = certificationService;
        this.ratingRepo = ratingRepo;
        this.professionalZoneRepo = professionalZoneRepo;
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
                "skills", cv.getSkills() != null ? cv.getSkills() : "",
                "workExperiences", cv.getProfessional().getWorkHistory() != null
                        ? cv.getProfessional().getWorkHistory().stream().map(this::toItem).toList()
                        : List.of(),
                "education", educationService.getEducationByProfessional(userId),
                "certifications", certificationService.getCertificationsByProfessional(userId),
                "zones", professionalZoneRepo.findByCvId(cv.getId())
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
     * Actualizar MIS habilidades (aptitudes)
     */
    @PutMapping("/me/skills")
    public ResponseEntity<?> updateMySkills(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los professionals pueden editar su CV"));
        }

        String skills = body.get("skills");
        Cv cv = cvService.updateSkills(userId, skills);
        Cv refreshed = cvService.getPublicCv(userId);

        return ResponseEntity.ok(toPublicResponse(refreshed));
    }

    // ========== NUEVOS ENDPOINTS - GUARDAR EXPERIENCIAS INDIVIDUALES ==========

    /**
     * Agregar o actualizar UNA SOLA experiencia laboral
     * POST /api/cv/{cvId}/work-experience
     */
    @PostMapping("/{cvId}/work-experience")
    public ResponseEntity<?> addOrUpdateWorkExperience(
            @PathVariable Long cvId,
            @RequestBody WorkHistoryRequest workExperience,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            String userType = (String) request.getAttribute("userType");

            if (userId == null || !"PROFESSIONAL".equals(userType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Solo los professionals pueden editar su CV"));
            }

            // Validar que el CV pertenece al usuario
            Cv cv = cvService.getCvById(cvId);
            if (!cv.getProfessional().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para editar este CV"));
            }

            // Validar máximo 3 trabajos activos
            if (workExperience.getIsActive() != null && workExperience.getIsActive()) {
                long activeJobsCount = workHistoryService.countActiveJobsByProfessional(userId);

                // Si está editando un trabajo existente que ya estaba activo, no contar ese
                if (workExperience.getBusinessId() != null) {
                    WorkHistory existing = workHistoryService.getById(workExperience.getBusinessId());
                    if (existing != null && existing.getIsActive()) {
                        activeJobsCount--; // No contar el que estamos editando
                    }
                }

                if (activeJobsCount >= 3) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Ya tienes 3 trabajos activos. Desactiva uno para agregar otro."));
                }
            }

            // Validar que tiene al menos position
            if (workExperience.getPosition() == null || workExperience.getPosition().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "El puesto es obligatorio"));
            }

            // Determinar si es crear o actualizar basándose en si viene businessId
            WorkHistory savedWork;
            Long workHistoryId = workExperience.getBusinessId(); // Asumo que usas businessId como identificador

            if (workHistoryId != null) {
                // Actualizar existente
                System.out.println("✏️ Actualizando work history ID: " + workHistoryId);
                savedWork = workHistoryService.updateWorkHistory(userId, workHistoryId, workExperience);
            } else {
                // Crear nuevo
                System.out.println("➕ Creando nueva work history: " + workExperience.getPosition());
                savedWork = workHistoryService.addWorkHistory(userId, workExperience);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Experiencia laboral guardada correctamente",
                    "workHistoryId", savedWork.getId(),
                    "isActive", savedWork.getIsActive()
            ));

        } catch (Exception e) {
            System.err.println("❌ Error guardando experiencia laboral: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar: " + e.getMessage()));
        }
    }

    /**
     * Eliminar UNA experiencia laboral
     * DELETE /api/cv/{cvId}/work-experience/{workHistoryId}
     */
    @DeleteMapping("/{cvId}/work-experience/{workHistoryId}")
    public ResponseEntity<?> deleteWorkExperience(
            @PathVariable Long cvId,
            @PathVariable Long workHistoryId,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            String userType = (String) request.getAttribute("userType");

            if (userId == null || !"PROFESSIONAL".equals(userType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Solo los professionals pueden editar su CV"));
            }

            // Validar que el CV pertenece al usuario
            Cv cv = cvService.getCvById(cvId);
            if (!cv.getProfessional().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para editar este CV"));
            }

            workHistoryService.deleteWorkHistory(userId, workHistoryId);

            return ResponseEntity.ok(Map.of("message", "Experiencia laboral eliminada correctamente"));

        } catch (Exception e) {
            System.err.println("❌ Error eliminando experiencia laboral: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar: " + e.getMessage()));
        }
    }

    // ========== ENDPOINTS PARA EDUCACIÓN INDIVIDUAL ==========

    /**
     * Agregar o actualizar UNA educación
     * POST /api/cv/{cvId}/education
     */
    @PostMapping("/{cvId}/education")
    public ResponseEntity<?> addOrUpdateEducation(
            @PathVariable Long cvId,
            @RequestBody Education education,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            String userType = (String) request.getAttribute("userType");

            if (userId == null || !"PROFESSIONAL".equals(userType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Solo los professionals pueden editar su CV"));
            }

            // Validar que el CV pertenece al usuario
            Cv cv = cvService.getCvById(cvId);
            if (!cv.getProfessional().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para editar este CV"));
            }

            // Validar que tenga al menos institución o título
            if ((education.getInstitution() == null || education.getInstitution().isEmpty()) &&
                    (education.getDegree() == null || education.getDegree().isEmpty())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Debe ingresar al menos la institución o el título"));
            }

            // Determinar si es crear o actualizar
            Education savedEducation;
            Long educationId = education.getId();

            if (educationId != null) {
                // Actualizar existente
                System.out.println("✏️ Actualizando educación ID: " + educationId);
                savedEducation = educationService.updateEducation(userId, educationId, education);
            } else {
                // Crear nueva
                System.out.println("➕ Creando nueva educación: " + education.getDegree());
                savedEducation = educationService.addEducation(userId, education);
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Educación guardada correctamente",
                    "educationId", savedEducation.getId()
            ));

        } catch (Exception e) {
            System.err.println("❌ Error guardando educación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar: " + e.getMessage()));
        }
    }

    /**
     * Eliminar UNA educación
     * DELETE /api/cv/{cvId}/education/{educationId}
     */
    @DeleteMapping("/{cvId}/education/{educationId}")
    public ResponseEntity<?> deleteEducation(
            @PathVariable Long cvId,
            @PathVariable Long educationId,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            String userType = (String) request.getAttribute("userType");

            if (userId == null || !"PROFESSIONAL".equals(userType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Solo los professionals pueden editar su CV"));
            }

            // Validar que el CV pertenece al usuario
            Cv cv = cvService.getCvById(cvId);
            if (!cv.getProfessional().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "No tienes permiso para editar este CV"));
            }

            educationService.deleteEducation(userId, educationId);

            return ResponseEntity.ok(Map.of("message", "Educación eliminada correctamente"));

        } catch (Exception e) {
            System.err.println("❌ Error eliminando educación: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar: " + e.getMessage()));
        }
    }

    // ========== ENDPOINT ORIGINAL - ACTUALIZAR CV COMPLETO ==========

    /**
     * Actualizar CV completo (workExperiences, education, certifications)
     * NOTA: Este endpoint ahora se usará principalmente para descripción, educación y certificaciones
     * Las experiencias laborales se guardan individualmente con los endpoints nuevos
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

            // 0. Actualizar descripción y habilidades en una sola transacción
            if (updates.containsKey("description") || updates.containsKey("skills")) {
                String description = updates.containsKey("description") ? (String) updates.get("description") : null;
                String skills = updates.containsKey("skills") ? (String) updates.get("skills") : null;
                Cv updated = cvService.updateDescriptionAndSkills(userId,
                        description != null ? description : "",
                        skills != null ? skills : "");
                System.out.println("✏️ Descripción: " + updated.getDescription() + " | Habilidades: " + updated.getSkills());
            }

            // 1. Actualizar work experiences (mantenido por compatibilidad)
            List<Map<String, Object>> workExperiences = (List<Map<String, Object>>) updates.get("workExperiences");
            if (workExperiences != null) {
                System.out.println("💼 Procesando " + workExperiences.size() + " experiencias laborales");

                for (Map<String, Object> exp : workExperiences) {
                    WorkHistoryRequest req = new WorkHistoryRequest();
                    req.setBusinessName((String) exp.get("businessName"));
                    req.setPosition((String) exp.get("position"));

                    // Parsear fechas de String a LocalDate
                    String startDateStr = (String) exp.get("startDate");
                    String endDateStr = (String) exp.get("endDate");

                    req.setStartDate(startDateStr != null && !startDateStr.isEmpty() ?
                            java.time.LocalDate.parse(startDateStr) : null);
                    req.setEndDate(endDateStr != null && !endDateStr.isEmpty() ?
                            java.time.LocalDate.parse(endDateStr) : null);

                    req.setDescription((String) exp.get("description"));
                    req.setReferenceContact((String) exp.get("referenceContact"));
                    req.setIsFreelance((Boolean) exp.getOrDefault("isFreelance", false));
                    req.setIsActive((Boolean) exp.getOrDefault("isActive", false));

                    Object workHistoryIdObj = exp.get("workHistoryId");
                    if (workHistoryIdObj != null) {
                        Long workHistoryId = ((Number) workHistoryIdObj).longValue();
                        System.out.println("  ✏️ Actualizando work history ID: " + workHistoryId);
                        workHistoryService.updateWorkHistory(userId, workHistoryId, req);
                    } else if (req.getPosition() != null && !req.getPosition().isEmpty()) {
                        System.out.println("  ➕ Creando nueva work history: " + req.getPosition() +
                                (req.getIsFreelance() ? " (Freelance)" : ""));
                        workHistoryService.addWorkHistory(userId, req);
                    } else {
                        System.out.println("  ⚠️ Trabajo ignorado: sin posición");
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

    // ========== OTROS ENDPOINTS ORIGINALES ==========

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
    public ResponseEntity<?> getPublic(@PathVariable Long professionalId) {
        try {
            Cv cv = cvService.getPublicCv(professionalId);
            return ResponseEntity.ok(toPublicResponse(cv));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
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
     * Generar CV en PDF con selección de contenido y layout (PÚBLICO)
     */
    @PostMapping("/{professionalId}/generate-pdf")
    public ResponseEntity<byte[]> generateCvPdf(
            @PathVariable Long professionalId,
            @RequestBody PdfGenerationRequest request) {
        try {
            byte[] pdfBytes = pdfService.generateCvPdf(professionalId, request);

            String professionalName = "Profesional";
            try {
                Cv cv = cvService.getPublicCv(professionalId);
                professionalName = cv.getProfessional().getName()
                        .replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\\s]", "")
                        .trim()
                        .replaceAll("\\s+", "_");
            } catch (Exception ignored) {}

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("CV_" + professionalName + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Preview de CV en PDF (inline, para mostrar en el browser) — requiere autenticación
     */
    @GetMapping("/me/preview-pdf")
    public ResponseEntity<byte[]> previewCvPdf(
            @RequestParam(defaultValue = "clasico") String layout,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String userType = (String) request.getAttribute("userType");

            if (userId == null || !"PROFESSIONAL".equals(userType)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            PdfGenerationRequest pdfRequest = new PdfGenerationRequest();
            pdfRequest.setLayout(layout);

            byte[] pdfBytes = pdfService.generateCvPdf(userId, pdfRequest);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("inline").build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Descargar CV en PDF (PÚBLICO)
     */
    @GetMapping("/{professionalId}/download-pdf")
    public ResponseEntity<byte[]> downloadCvPdf(@PathVariable Long professionalId) {
        try {
            byte[] pdfBytes = pdfService.generateCvPdf(professionalId);

            String professionalName = "Profesional";
            try {
                Cv cv = cvService.getPublicCv(professionalId);
                professionalName = cv.getProfessional().getName()
                        .replaceAll("[^a-zA-ZáéíóúÁÉÍÓÚüÜñÑ\\s]", "")
                        .trim()
                        .replaceAll("\\s+", "_");
            } catch (Exception ignored) {}

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.builder("attachment")
                            .filename("CV_" + professionalName + ".pdf")
                            .build()
            );

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== ENDPOINTS DE ZONAS DE TRABAJO ==========

    @GetMapping("/{cvId}/zones")
    public ResponseEntity<?> getZones(@PathVariable Long cvId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado"));
        }

        Cv cv = cvService.getCvById(cvId);
        if (!cv.getProfessional().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tenés permiso para ver estas zonas"));
        }

        return ResponseEntity.ok(professionalZoneRepo.findByCvId(cvId));
    }

    @PostMapping("/{cvId}/zones")
    public ResponseEntity<?> addZone(
            @PathVariable Long cvId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado"));
        }

        Cv cv = cvService.getCvById(cvId);
        if (!cv.getProfessional().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tenés permiso para editar estas zonas"));
        }

        String provincia = body.get("provincia");
        String zona = body.get("zona");

        if (provincia == null || provincia.isBlank() || zona == null || zona.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Provincia y zona son requeridas"));
        }

        // Verificar que no exista la misma zona ya cargada
        boolean yaExiste = professionalZoneRepo.findByCvId(cvId).stream()
                .anyMatch(z -> z.getZona().equalsIgnoreCase(zona) && z.getProvincia().equalsIgnoreCase(provincia));

        if (yaExiste) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Esa zona ya está agregada"));
        }

        ProfessionalZone zone = ProfessionalZone.builder()
                .cv(cv)
                .provincia(provincia)
                .zona(zona)
                .build();

        ProfessionalZone saved = professionalZoneRepo.save(zone);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{cvId}/zones/{zoneId}")
    public ResponseEntity<?> deleteZone(
            @PathVariable Long cvId,
            @PathVariable Long zoneId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"PROFESSIONAL".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No autorizado"));
        }

        Cv cv = cvService.getCvById(cvId);
        if (!cv.getProfessional().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "No tenés permiso"));
        }

        professionalZoneRepo.deleteById(zoneId);
        return ResponseEntity.ok(Map.of("message", "Zona eliminada correctamente"));
    }

    // ========== MAPPERS ==========

    private CvPublicResponse toPublicResponse(Cv cv) {
        AppUser p = cv.getProfessional();

        CvPublicResponse dto = new CvPublicResponse();
        dto.setProfessionalId(p.getId());
        dto.setProfessionalName(p.getName());
        dto.setProfessionalEmail(p.getEmail());
        dto.setProfessionalPhone(p.getPhone());
        dto.setProfessionalLocation(p.getLocation());
        dto.setProfilePicture(p.getProfilePicture());
        dto.setProfessionType(p.getProfessionType());
        dto.setProfessionalTitle(p.getProfessionalTitle());

        dto.setDescription(cv.getDescription());
        dto.setSkills(cv.getSkills());
        dto.setReputationScore(cv.getReputationScore() != null ? cv.getReputationScore().doubleValue() : 0.0);
        dto.setTotalRatings(cv.getTotalRatings());

        // Historial laboral
        dto.setWorkHistory(p.getWorkHistory() != null
                ? p.getWorkHistory().stream().map(this::toItem).toList()
                : List.of());

        // Education y Certifications
        dto.setEducation(educationService.getEducationByProfessional(p.getId()));
        dto.setCertifications(certificationService.getCertificationsByProfessional(p.getId()));

        List<ZoneResponse> zones = professionalZoneRepo.findByCvId(cv.getId()).stream()
                .map(z -> { ZoneResponse zr = new ZoneResponse(); zr.setId(z.getId()); zr.setProvincia(z.getProvincia()); zr.setZona(z.getZona()); return zr; })
                .toList();
        dto.setZones(zones);

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
        item.setReferencePhone(wh.getReferencePhone());

        long ratingCount = ratingRepo.countByWorkHistoryId(wh.getId());
        item.setTotalRatings((int) ratingCount);

        return item;
    }
}