package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.SwitchRoleRequest;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.service.AuthService;
import com.example.waiter_rating.service.JwtService;
import com.example.waiter_rating.service.RoleSwitchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/switch")
    public ResponseEntity<?> switchRole(@Valid @RequestBody SwitchRoleRequest request) {
        AppUser currentUser = authService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Debe estar autenticado"));

        AppUser updatedUser = roleSwitchService.switchRole(
                currentUser.getId(),
                request.getNewRole(),
                request.getEffectiveProfessionTypes(),
                request.getProfessionalTitle()
        );

        String newToken = jwtService.generateToken(
                updatedUser.getId(),
                request.getNewRole().toString(),
                updatedUser.getEmail(),
                updatedUser.getName()
        );

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

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentRole() {
        try {
            System.out.println("=== /api/role/current called ===");
            
            AppUser currentUser = authService.getCurrentUser()
                    .orElseThrow(() -> new IllegalStateException("Debe estar autenticado"));
            
            System.out.println("✅ Current user: " + currentUser.getEmail());
            
            LocalDateTime nextAllowedSwitch = currentUser.getNextAllowedRoleSwitchDate();
            System.out.println("Next allowed switch: " + nextAllowedSwitch);
            
            boolean canSwitch = currentUser.canSwitchRole();
            System.out.println("Can switch: " + canSwitch);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            return ResponseEntity.ok(Map.of(
                    "activeRole", currentUser.getActiveRole().toString(),
                    "userId", currentUser.getId(),
                    "email", currentUser.getEmail(),
                    "name", currentUser.getName(),
                    "canSwitchRole", canSwitch,
                    "nextAllowedRoleSwitchDate", nextAllowedSwitch != null ? nextAllowedSwitch.format(formatter) : "now",
                    "lastRoleSwitchAt", currentUser.getLastRoleSwitchAt() != null ? currentUser.getLastRoleSwitchAt().format(formatter) : "never"
            ));
        } catch (Exception e) {
            System.err.println("❌ ERROR in /api/role/current: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage(), "stackTrace", e.toString()));
        }
    }
}
