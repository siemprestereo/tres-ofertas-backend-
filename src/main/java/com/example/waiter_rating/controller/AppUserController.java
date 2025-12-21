package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.AppUserRequest;
import com.example.waiter_rating.dto.response.AppUserResponse;
import com.example.waiter_rating.service.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService userService;

    public AppUserController(AppUserService userService) {
        this.userService = userService;
    }

    /** Crear usuario (redirige a Client o Professional según userType) */
    @PostMapping
    public ResponseEntity<AppUserResponse> create(@Valid @RequestBody AppUserRequest request) {
        AppUserResponse response = userService.create(request);
        return ResponseEntity.ok(response);
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
}