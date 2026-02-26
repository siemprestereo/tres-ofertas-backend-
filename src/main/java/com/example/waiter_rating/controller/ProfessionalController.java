package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.ProfessionalRequest;
import com.example.waiter_rating.dto.response.ProfessionalResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.ProfessionType;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.service.ProfessionalService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
@Slf4j
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final ProfessionalService professionalService;
    private final AppUserRepo appUserRepo;

    public ProfessionalController(ProfessionalService professionalService, AppUserRepo appUserRepo) {
        this.professionalService = professionalService;
        this.appUserRepo = appUserRepo;
    }

    @PostMapping
    public ResponseEntity<ProfessionalResponse> create(@Valid @RequestBody ProfessionalRequest request) {
        ProfessionalResponse response = professionalService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProfessionals(
            @RequestParam String query,
            @RequestParam(required = false) String location) {  // ← AGREGAR
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            String searchTerm = query.toLowerCase().trim();
            String locationTerm = (location != null && !location.isBlank())
                    ? location.toLowerCase().trim()
                    : null;

            List<AppUser> professionals = appUserRepo.findSearchableProfessionals().stream()
                    .filter(p -> p.getWorkHistory() != null &&
                            p.getWorkHistory().stream().anyMatch(wh -> wh.getIsActive()))
                    .filter(p -> {
                        if (p.getName() != null && p.getName().toLowerCase().contains(searchTerm)) return true;
                        if (p.getProfessionType() != null) {
                            String professionName = translateProfession(p.getProfessionType());
                            if (professionName.toLowerCase().contains(searchTerm)) return true;
                        }
                        if (p.getLocation() != null && p.getLocation().toLowerCase().contains(searchTerm)) return true;
                        return false;
                    })
                    // ← AGREGAR este filtro
                    .filter(p -> {
                        if (locationTerm == null) return true;
                        return p.getLocation() != null && p.getLocation().toLowerCase().contains(locationTerm);
                    })
                    .collect(Collectors.toList());

            List<Map<String, Object>> response = professionals.stream()
                    .map(this::toSearchResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error en búsqueda: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en búsqueda: " + e.getMessage()));
        }
    }


    @GetMapping("/search/top")
    public ResponseEntity<?> getTopProfessionals() {
        try {
            List<AppUser> topProfessionals = appUserRepo.findTopProfessionals(PageRequest.of(0, 20)).stream()
                    .filter(p -> p.getWorkHistory() != null &&
                            p.getWorkHistory().stream().anyMatch(wh -> wh.getIsActive()))
                    .collect(Collectors.toList());

            List<Map<String, Object>> response = topProfessionals.stream()
                    .map(this::toSearchResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error obteniendo top: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error obteniendo top: " + e.getMessage()));
        }
    }

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

            Optional<AppUser> professionalOpt = appUserRepo.findByEmail(email);

            if (professionalOpt.isEmpty() || !UserRole.PROFESSIONAL.equals(professionalOpt.get().getActiveRole())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            AppUser professional = professionalOpt.get();
            Boolean searchable = professional.getSearchable();

            return ResponseEntity.ok(Map.of("searchable", searchable != null ? searchable : false));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener estado: " + e.getMessage()));
        }
    }

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

            Optional<AppUser> professionalOpt = appUserRepo.findByEmail(email);

            if (professionalOpt.isEmpty() || !UserRole.PROFESSIONAL.equals(professionalOpt.get().getActiveRole())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            AppUser professional = professionalOpt.get();

            Boolean searchable = request.get("searchable");
            if (searchable == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El campo 'searchable' es requerido"));
            }

            professional.setSearchable(searchable);
            appUserRepo.save(professional);

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

    @GetMapping("/searchable-status")
    public ResponseEntity<?> getSearchableStatus() {
        return getMySearchableStatus();
    }

    @PutMapping("/toggle-searchable")
    public ResponseEntity<?> toggleSearchable() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || authentication.getPrincipal() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "No autenticado"));
            }

            String email = authentication.getPrincipal().toString();

            Optional<AppUser> professionalOpt = appUserRepo.findByEmail(email);

            if (professionalOpt.isEmpty() || !UserRole.PROFESSIONAL.equals(professionalOpt.get().getActiveRole())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            AppUser professional = professionalOpt.get();

            Boolean currentValue = professional.getSearchable();
            Boolean newValue = (currentValue == null || !currentValue);
            professional.setSearchable(newValue);

            appUserRepo.save(professional);

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

    @GetMapping("/email/{email}")
    public ResponseEntity<ProfessionalResponse> getByEmail(@PathVariable String email) {
        return professionalService.getByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ProfessionalResponse>> listAll(
            @RequestParam(required = false) ProfessionType professionType) {

        if (professionType != null) {
            return ResponseEntity.ok(professionalService.listByProfessionType(professionType));
        }
        return ResponseEntity.ok(professionalService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> getById(@PathVariable Long id) {
        return professionalService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProfessionalRequest request) {
        ProfessionalResponse response = professionalService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        professionalService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/can-change-workplace")
    public ResponseEntity<Map<String, Boolean>> canChangeWorkplace(@PathVariable Long id) {
        boolean canChange = professionalService.canChangeWorkplace(id);
        return ResponseEntity.ok(Map.of("canChange", canChange));
    }

    @PostMapping("/{id}/register-workplace-change")
    public ResponseEntity<Map<String, String>> registerWorkplaceChange(@PathVariable Long id) {
        professionalService.registerWorkplaceChange(id);
        return ResponseEntity.ok(Map.of("message", "Cambio de lugar de trabajo registrado exitosamente"));
    }

    private Map<String, Object> toSearchResponse(AppUser p) {
        return Map.of(
                "id", p.getId(),
                "name", p.getName() != null ? p.getName() : "",
                "professionType", p.getProfessionType() != null ? p.getProfessionType().toString() : "",
                "location", p.getLocation() != null ? p.getLocation() : "",
                "reputationScore", p.getReputationScore() != null ? p.getReputationScore() : 0.0,
                "totalRatings", p.getTotalRatings() != null ? p.getTotalRatings() : 0
        );
    }

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