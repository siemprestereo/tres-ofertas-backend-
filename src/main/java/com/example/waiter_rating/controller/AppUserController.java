package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.response.AppUserResponse;
import com.example.waiter_rating.service.AppUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService userService;

    public AppUserController(AppUserService userService) {
        this.userService = userService;
    }

    /** Obtener usuario por ID (genérico - puede ser Client o Professional) */
    @GetMapping("/{id}")
    public ResponseEntity<AppUserResponse> getById(@PathVariable Long id) {
        AppUserResponse response = userService.getById(id);
        return ResponseEntity.ok(response);
    }

    /** Listar todos los usuarios (Clients y Professionals) */
    @GetMapping
    public ResponseEntity<List<AppUserResponse>> listAll() {
        return ResponseEntity.ok(userService.listAll());
    }

    /** Verificar roles del usuario autenticado */
    @GetMapping("/me/roles")
    public ResponseEntity<Map<String, Object>> checkMyRoles(@RequestHeader("Authorization") String authHeader) {
        Map<String, Object> roles = userService.checkUserRoles(authHeader);
        return ResponseEntity.ok(roles);
    }
}