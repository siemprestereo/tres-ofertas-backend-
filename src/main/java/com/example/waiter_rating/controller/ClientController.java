package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.request.ClientRequest;
import com.example.waiter_rating.dto.response.ClientResponse;
import com.example.waiter_rating.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
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
}