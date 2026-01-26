package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.ClientRequest;
import com.example.waiter_rating.dto.response.ClientResponse;
import com.example.waiter_rating.dto.response.FavoriteProfessionalResponse;
import com.example.waiter_rating.service.ClientService;
import com.example.waiter_rating.service.FavoriteProfessionalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    private final FavoriteProfessionalService favoriteProfessionalService;

    public ClientController(ClientService clientService, FavoriteProfessionalService favoriteProfessionalService) {
        this.clientService = clientService;
        this.favoriteProfessionalService = favoriteProfessionalService;
    }

    /** Registrar un nuevo cliente */
    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        ClientResponse response = clientService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Obtener cliente por ID */
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getById(@PathVariable Long id) {
        return clientService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Obtener cliente por email */
    @GetMapping("/email/{email}")
    public ResponseEntity<ClientResponse> getByEmail(@PathVariable String email) {
        return clientService.getByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Listar todos los clientes */
    @GetMapping
    public ResponseEntity<List<ClientResponse>> listAll() {
        return ResponseEntity.ok(clientService.listAll());
    }

    /** Actualizar datos del cliente */
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClientRequest request) {
        ClientResponse response = clientService.update(id, request);
        return ResponseEntity.ok(response);
    }

    /** Eliminar cliente */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ========== FAVORITOS ==========



    // Modificar el constructor para incluir el servicio


    /**
     * Agregar profesional a favoritos
     * POST /api/clients/me/favorites/{professionalId}
     */
    @PostMapping("/me/favorites/{professionalId}")
    public ResponseEntity<?> addFavorite(
            @PathVariable Long professionalId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"CLIENT".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los clientes pueden guardar favoritos"));
        }

        try {
            String notes = body != null ? body.get("notes") : null;
            FavoriteProfessionalResponse response = favoriteProfessionalService.addFavorite(
                    userId,
                    professionalId,
                    notes
            );
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al agregar favorito: " + e.getMessage()));
        }
    }

    /**
     * Quitar profesional de favoritos
     * DELETE /api/clients/me/favorites/{professionalId}
     */
    @DeleteMapping("/me/favorites/{professionalId}")
    public ResponseEntity<?> removeFavorite(
            @PathVariable Long professionalId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"CLIENT".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los clientes pueden gestionar favoritos"));
        }

        try {
            favoriteProfessionalService.removeFavorite(userId, professionalId);
            return ResponseEntity.ok(Map.of("message", "Profesional eliminado de favoritos"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar favorito: " + e.getMessage()));
        }
    }

    /**
     * Verificar si un profesional está en favoritos
     * GET /api/clients/me/favorites/{professionalId}/check
     */
    @GetMapping("/me/favorites/{professionalId}/check")
    public ResponseEntity<?> checkFavorite(
            @PathVariable Long professionalId,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"CLIENT".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los clientes pueden ver favoritos"));
        }

        boolean isFavorite = favoriteProfessionalService.isFavorite(userId, professionalId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    /**
     * Listar todos los favoritos
     * GET /api/clients/me/favorites
     */
    @GetMapping("/me/favorites")
    public ResponseEntity<?> listFavorites(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"CLIENT".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los clientes pueden ver favoritos"));
        }

        try {
            List<FavoriteProfessionalResponse> favorites;

            if (startDate != null && endDate != null) {
                LocalDate start = LocalDate.parse(startDate);
                LocalDate end = LocalDate.parse(endDate);
                favorites = favoriteProfessionalService.listFavoritesWithStats(userId, start, end);
            } else {
                favorites = favoriteProfessionalService.listFavorites(userId);
            }

            return ResponseEntity.ok(favorites);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener favoritos: " + e.getMessage()));
        }
    }

    /**
     * Actualizar notas de un favorito
     * PUT /api/clients/me/favorites/{professionalId}/notes
     */
    @PutMapping("/me/favorites/{professionalId}/notes")
    public ResponseEntity<?> updateNotes(
            @PathVariable Long professionalId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (userId == null || !"CLIENT".equals(userType)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Solo los clientes pueden actualizar notas"));
        }

        try {
            String notes = body.get("notes");
            FavoriteProfessionalResponse response = favoriteProfessionalService.updateNotes(
                    userId,
                    professionalId,
                    notes
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al actualizar notas: " + e.getMessage()));
        }
    }
}