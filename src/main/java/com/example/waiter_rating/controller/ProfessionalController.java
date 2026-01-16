package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.ProfessionalRequest;
import com.example.waiter_rating.dto.response.ProfessionalResponse;
import com.example.waiter_rating.model.ProfessionType;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.repository.ProfessionalRepo;
import com.example.waiter_rating.service.ProfessionalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /** Obtener professional por ID */
    @GetMapping("/{id}")
    public ResponseEntity<ProfessionalResponse> getById(@PathVariable Long id) {
        return professionalService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    // Agregar este endpoint al ProfessionalController.java

    @PutMapping("/api/professionals/toggle-searchable")
    public ResponseEntity<?> toggleSearchable(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String email = userDetails.getUsername();

            // Buscar el profesional por email
            Optional<Professional> professionalOpt = professionalRepo.findByEmail(email);

            if (professionalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            Professional professional = professionalOpt.get();

            // Toggle del campo searchable
            professional.setSearchable(!professional.getSearchable());

            // Guardar cambios
            professionalRepo.save(professional);

            // Retornar estado actualizado
            return ResponseEntity.ok(Map.of(
                    "searchable", professional.getSearchable(),
                    "message", professional.getSearchable()
                            ? "Perfil activado en búsquedas"
                            : "Perfil desactivado en búsquedas"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar configuración: " + e.getMessage()));
        }
    }

    // También agregar un endpoint GET para obtener el estado actual
    @GetMapping("/api/professionals/searchable-status")
    public ResponseEntity<?> getSearchableStatus(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            String email = userDetails.getUsername();

            Optional<Professional> professionalOpt = professionalRepo.findByEmail(email);

            if (professionalOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Profesional no encontrado"));
            }

            Professional professional = professionalOpt.get();

            return ResponseEntity.ok(Map.of("searchable", professional.getSearchable()));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener estado: " + e.getMessage()));
        }
    }
}