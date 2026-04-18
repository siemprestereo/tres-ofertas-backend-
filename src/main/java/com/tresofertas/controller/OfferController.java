package com.tresofertas.controller;

import com.tresofertas.dto.request.CreateOfferRequest;
import com.tresofertas.dto.response.OfferResponse;
import com.tresofertas.service.OfferService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/merchants/{merchantId}")
    public ResponseEntity<OfferResponse> create(@PathVariable Long merchantId,
                                                @RequestBody CreateOfferRequest request,
                                                HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(offerService.create(merchantId, request, userId));
    }

    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<List<OfferResponse>> getByMerchant(@PathVariable Long merchantId) {
        return ResponseEntity.ok(offerService.getByMerchant(merchantId));
    }

    @DeleteMapping("/{offerId}")
    public ResponseEntity<OfferResponse> deactivate(@PathVariable Long offerId,
                                                    HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(offerService.deactivate(offerId, userId));
    }

    @GetMapping
    public ResponseEntity<List<OfferResponse>> getPublicFeed(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(offerService.getPublicFeed(category));
    }

    @GetMapping("/feed")
    public ResponseEntity<List<OfferResponse>> getPersonalizedFeed(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(offerService.getPersonalizedFeed(userId));
    }
}
