package com.tresofertas.controller;

import com.tresofertas.dto.request.CreateMerchantRequest;
import com.tresofertas.dto.response.MerchantResponse;
import com.tresofertas.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<MerchantResponse> create(@RequestBody CreateMerchantRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantService.create(request, userId));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<MerchantResponse> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(merchantService.getBySlug(slug));
    }

    @GetMapping("/me")
    public ResponseEntity<List<MerchantResponse>> getMyMerchants(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(merchantService.getMyMerchants(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MerchantResponse> update(@PathVariable Long id,
                                                   @RequestBody CreateMerchantRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(merchantService.update(id, request, userId));
    }
}
