package com.example.waiter_rating.controller;

import com.example.waiter_rating.model.Business;
import com.example.waiter_rating.service.BusinessService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    /** Crear restaurante */
    @PostMapping
    public ResponseEntity<Business> create(@RequestBody Business business) {
        Business created = businessService.create(business);
        return ResponseEntity
                .created(URI.create("/api/restaurants/" + created.getId()))
                .body(created);
    }

    /** Obtener restaurante por id */
    @GetMapping("/{id}")
    public ResponseEntity<Business> getById(@PathVariable Long id) {
        return ResponseEntity.ok(businessService.getById(id));
    }

    /** Listar todos los restaurantes */
    @GetMapping
    public ResponseEntity<List<Business>> listAll() {
        return ResponseEntity.ok(businessService.listAll());
    }
}

