package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.SwitchRoleRequest;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.JwtService;
import com.example.waiter_rating.service.RoleSwitchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/role")
public class RoleSwitchController {

    private final RoleSwitchService roleSwitchService;
    private final AuthService authService;
    private final JwtService jwtService;

    public RoleSwitchController(RoleSwitchService roleSwitchService,
                                AuthService authService,
                                JwtService jwtService) {
        this.roleSwitchService = roleSwitchService;
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * Cambia el rol activo del usuario autenticado
     */
    @PostMapping("/switch")
    public ResponseEntity<?> switchRole(@Valid @RequestBody SwitchRoleRequest request) {
        // Obtener el usuario actual (puede ser Client o Professional)
        AppUser currentUser = authService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado"));

        // Realizar el cambio de rol
        AppUser updatedUser = roleSwitchService.switchRole(
                currentUser.getId(),
                request.getNewRole(),
                request.getProfessionType(),
                request.getProfessionalTitle()
        );

        // Generar nuevo JWT con el nuevo rol
        String newToken = jwtService.generateToken(
                updatedUser.getId(),
                request.getNewRole().toString(),
                updatedUser.getEmail(),
                updatedUser.getName()
        );

        // Determinar a dónde redirigir según el nuevo rol
        String redirectTo = request.getNewRole() == UserRole.PROFESSIONAL
                ? "/professional-dashboard"
                : "/client-dashboard";

        return ResponseEntity.ok(Map.of(
                "message", "Rol cambiado exitosamente a " + request.getNewRole(),
                "token", newToken,
                "newRole", request.getNewRole().toString(),
                "redirectTo", redirectTo
        ));
    }

    /**
     * Obtiene el rol activo actual del usuario
     */
    /**
     * Obtiene el rol activo actual del usuario y la info de cambio de rol
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentRole() {
        AppUser currentUser = authService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado"));

        LocalDateTime nextAllowedSwitch = currentUser.getNextAllowedRoleSwitchDate();
        boolean canSwitch = currentUser.canSwitchRole();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        return ResponseEntity.ok(Map.of(
                "activeRole", currentUser.getActiveRole(),
                "userId", currentUser.getId(),
                "email", currentUser.getEmail(),
                "name", currentUser.getName(),
                "canSwitchRole", canSwitch,
                "nextAllowedRoleSwitchDate", nextAllowedSwitch != null ? nextAllowedSwitch.format(formatter) : null,
                "lastRoleSwitchAt", currentUser.getLastRoleSwitchAt() != null ? currentUser.getLastRoleSwitchAt().format(formatter) : null
        ));
    }

}