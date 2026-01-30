package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.ProfessionalRequest;
import com.example.waiter_rating.dto.response.ProfessionalResponse;
import com.example.waiter_rating.model.ProfessionType;
import com.example.waiter_rating.service.ProfessionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final ProfessionalService professionalService;
    private final ProfessionalRepo professionalRepo;

    public ProfessionalController(ProfessionalService professionalService, ProfessionalRepo professionalRepo) {
        this.professionalService = professionalService;
        this.professionalRepo = professionalRepo;
    }

    /** Registrar un nuevo professional */
    @PostMapping
    public ResponseEntity<ProfessionalResponse> create(@Valid @RequestBody ProfessionalRequest request) {
        ProfessionalResponse response = professionalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================
    // ENDPOINTS DE BÚSQUEDA - NUEVOS
    // ============================================

    /**
     * Buscar profesionales por query (nombre, profesión, ubicación)
     * Solo devuelve profesionales que:
     * - searchable = true
     * - Tienen al menos un trabajo activo
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchProfessionals(@RequestParam String query) {
        try {
            System.out.println("🔍 Búsqueda: " + query);

            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            String searchTerm = query.toLowerCase().trim();

            // Buscar profesionales searchable con trabajos activos
            List<Professional> professionals = professionalRepo.findAll().stream()
                    .filter(p -> p.getSearchable() != null && p.getSearchable())
                    .filter(p -> p.getWorkHistory() != null &&
                            p.getWorkHistory().stream().anyMatch(wh -> wh.getIsActive()))
                    .filter(p -> {
                        // Buscar por nombre
                        if (p.getName() != null && p.getName().toLowerCase().contains(searchTerm)) {
                            return true;
                        }
                        // Buscar por tipo de profesión
                        if (p.getProfessionType() != null) {
                            String professionName = translateProfession(p.getProfessionType());
                            if (professionName.toLowerCase().contains(searchTerm)) {
                                return true;
                            }
                        }
                        // Buscar por ubicación
                        if (p.getLocation() != null && p.getLocation().toLowerCase().contains(searchTerm)) {
                            return true;
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            System.out.println("✅ Encontrados: " + professionals.size() + " profesionales");

            // Mapear a respuesta simplificada
            List<Map<String, Object>> response = professionals.stream()
                    .map(this::toSearchResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error en búsqueda: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en búsqueda: " + e.getMessage()));
        }
    }

    /**
     * Obtener top profesionales (mejores calificados)
     * Solo devuelve profesionales que:
     * - searchable = true
     * - Tienen al menos un trabajo activo
     * - Ordenados por reputationScore descendente
     */
    @GetMapping("/search/top")
    public ResponseEntity<?> getTopProfessionals() {
        try {
            System.out.println("🌟 Obteniendo top profesionales");

            List<Professional> topProfessionals = professionalRepo.findAll().stream()
                    .filter(p -> p.getSearchable() != null && p.getSearchable())
                    .filter(p -> p.getWorkHistory() != null &&
                            p.getWorkHistory().stream().anyMatch(wh -> wh.getIsActive()))
                    .filter(p -> p.getReputationScore() != null && p.getReputationScore() > 0)
                    .sorted((p1, p2) -> Double.compare(
                            p2.getReputationScore() != null ? p2.getReputationScore() : 0.0,
                            p1.getReputationScore() != null ? p1.getReputationScore() : 0.0
                    ))
                    .limit(20)
                    .collect(Collectors.toList());

            System.out.println("✅ Top profesionales: " + topProfessionals.size());

            List<Map<String, Object>> response = topProfessionals.stream()
                    .map(this::toSearchResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error obteniendo top: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error obteniendo top: " + e.getMessage()));
        }
    }

    // ============================================
    // ENDPOINTS CON RUTAS ESPECÍFICAS
    // ============================================

    /** Obtener estado searchable del usuario autenticado */
    @GetMapping("/me/searchable-status")
    public ResponseEntity<?> getMySearchableStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            String email = authentication.getPrincipal().toString();
            System.out.println("✅ Email obtenido: " + email);

            Optional<Professional> professionalOpt = professionalRepo.findByEmail(email);

            if (professionalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            Professional professional = professionalOpt.get();
            Boolean searchable = professional.getSearchable();

            return ResponseEntity.ok(Map.of("searchable", searchable != null ? searchable : false));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener estado: " + e.getMessage()));
        }
    }

    /** Actualizar estado searchable del usuario autenticado */
    @PutMapping("/me/searchable")
    public ResponseEntity<?> updateMySearchable(@RequestBody Map<String, Boolean> request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            String email = authentication.getPrincipal().toString();
            System.out.println("✅ Email obtenido: " + email);

            Optional<Professional> professionalOpt = professionalRepo.findByEmail(email);

            if (professionalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            Professional professional = professionalOpt.get();

            Boolean searchable = request.get("searchable");
            if (searchable == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El campo 'searchable' es requerido"));
            }

            professional.setSearchable(searchable);
            professionalRepo.save(professional);

            return ResponseEntity.ok(Map.of(
                    "searchable", searchable,
                    "message", searchable
                            ? "Perfil visible en búsquedas"
                            : "Perfil oculto de búsquedas"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar: " + e.getMessage()));
        }
    }

    /** Obtener estado searchable (sin ID en path) - LEGACY */
    @GetMapping("/searchable-status")
    public ResponseEntity<?> getSearchableStatus() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            String email = authentication.getPrincipal().toString();
            System.out.println("✅ Email obtenido: " + email);

            Optional<Professional> professionalOpt = professionalRepo.findByEmail(email);

            if (professionalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            Professional professional = professionalOpt.get();
            Boolean searchable = professional.getSearchable();

            return ResponseEntity.ok(Map.of("searchable", searchable != null ? searchable : false));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener estado: " + e.getMessage()));
        }
    }

    /** Toggle searchable (sin ID en path) - LEGACY */
    @PutMapping("/toggle-searchable")
    public ResponseEntity<?> toggleSearchable() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            String email = authentication.getPrincipal().toString();
            System.out.println("✅ Email obtenido: " + email);

            Optional<Professional> professionalOpt = professionalRepo.findByEmail(email);

            if (professionalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            Professional professional = professionalOpt.get();

            Boolean currentValue = professional.getSearchable();
            Boolean newValue = (currentValue == null || !currentValue) ? true : false;
            professional.setSearchable(newValue);

            professionalRepo.save(professional);

            return ResponseEntity.ok(Map.of(
                    "searchable", professional.getSearchable(),
                    "message", professional.getSearchable()
                            ? "Perfil activado en búsquedas"
                            : "Perfil desactivado en búsquedas"
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar configuración: " + e.getMessage()));
        }
    }

    /** Obtener professional por email */
    @GetMapping("/email/{email}")
    public ResponseEntity<ProfessionalResponse> getByEmail(@PathVariable String email) {
        return professionalService.getByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Listar todos los professionals */
    @GetMapping
    public ResponseEntity<List<ProfessionalResponse>> listAll(
            @RequestParam(required = false) ProfessionType professionType) {

        if (professionType != null) {
            return ResponseEntity.ok(professionalService.listByProfessionType(professionType));
        }
        return ResponseEntity.ok(professionalService.listAll());
    }

    // ============================================
    // ENDPOINTS CON {id} - AL FINAL
    // ============================================

    /** Obtener professional por ID */
    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> getById(@PathVariable Long id) {
        return professionalService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Actualizar datos del professional */
    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalRequest request) {
        ProfessionalResponse response = professionalService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /** Eliminar professional */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        professionalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Verificar si puede cambiar de lugar de trabajo */
    @GetMapping("/{id}/can-change-workplace")
    public ResponseEntity<Map<String, Boolean>> canChangeWorkplace(@PathVariable Long id) {
        boolean canChange = professionalService.canChangeWorkplace(id);
        return ResponseEntity.ok(Map.of("canChange", canChange));
    }

    /** Registrar un cambio de lugar de trabajo */
    @PostMapping("/{id}/register-workplace-change")
    public ResponseEntity<Map<String, String>> registerWorkplaceChange(@PathVariable Long id) {
        professionalService.registerWorkplaceChange(id);
        return ResponseEntity.ok(Map.of("message", "Cambio de lugar de trabajo registrado exitosamente"));
    }

    // ============================================
    // MÉTODOS AUXILIARES
    // ============================================

    /**
     * Convierte un Professional a un formato simplificado para búsquedas
     */
    private Map<String, Object> toSearchResponse(Professional p) {
        return Map.of(
                "id", p.getId(),
                "name", p.getName() != null ? p.getName() : "",
                "professionType", p.getProfessionType() != null ? p.getProfessionType().toString() : "",
                "location", p.getLocation() != null ? p.getLocation() : "",
                "reputationScore", p.getReputationScore() != null ? p.getReputationScore() : 0.0,
                "totalRatings", p.getTotalRatings() != null ? p.getTotalRatings() : 0
        );
    }

    /**
     * Traduce el tipo de profesión al español
     */
    private String translateProfession(ProfessionType type) {
        switch (type) {
            case WAITER: return "Mozo";
            case ELECTRICIAN: return "Electricista";
            case PAINTER: return "Pintor";
            case HAIRDRESSER: return "Peluquero";
            case PLUMBER: return "Plomero";
            case CARPENTER: return "Carpintero";
            case MECHANIC: return "Mecánico";
            case CHEF: return "Chef";
            case BARISTA: return "Barista";
            case BARTENDER: return "Bartender";
            case CLEANER: return "Personal de limpieza";
            case GARDENER: return "Jardinero";
            case DRIVER: return "Conductor";
            case SECURITY: return "Seguridad";
            case RECEPTIONIST: return "Recepcionista";
            default: return type.toString();
        }
    }
}