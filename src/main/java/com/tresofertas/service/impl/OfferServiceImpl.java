package com.tresofertas.service.impl;

import com.tresofertas.dto.request.CreateOfferRequest;
import com.tresofertas.dto.response.OfferResponse;
import com.tresofertas.model.Merchant;
import com.tresofertas.model.Offer;
import com.tresofertas.repository.MerchantRepo;
import com.tresofertas.repository.OfferRepo;
import com.tresofertas.service.OfferService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OfferServiceImpl implements OfferService {

    private final OfferRepo offerRepo;
    private final MerchantRepo merchantRepo;

    public OfferServiceImpl(OfferRepo offerRepo, MerchantRepo merchantRepo) {
        this.offerRepo = offerRepo;
        this.merchantRepo = merchantRepo;
    }

    @Override
    public OfferResponse create(Long merchantId, CreateOfferRequest request, Long userId) {
        Merchant merchant = merchantRepo.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Comercio no encontrado"));

        if (!merchant.getUser().getId().equals(userId)) {
            throw new RuntimeException("No tenés permiso para agregar ofertas a este comercio");
        }

        if (!merchant.getActive() || merchant.getSuspended()) {
            throw new RuntimeException("El comercio no está activo");
        }

        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new IllegalArgumentException("La descripción es requerida");
        }
        if (request.getExpiresAt() == null && (request.getUntilStockOut() == null || !request.getUntilStockOut())) {
            throw new IllegalArgumentException("Debe indicar una fecha de expiración o marcar 'hasta agotar stock'");
        }

        Offer offer = new Offer();
        offer.setMerchant(merchant);
        offer.setCode(generateCode());
        offer.setDescription(request.getDescription());
        offer.setPrice(request.getPrice());
        offer.setUntilStockOut(request.getUntilStockOut() != null && request.getUntilStockOut());
        offer.setExpiresAt(request.getExpiresAt() != null ? request.getExpiresAt()
                : java.time.LocalDateTime.now().plusYears(10));

        return OfferResponse.from(offerRepo.save(offer));
    }

    @Override
    public List<OfferResponse> getByMerchant(Long merchantId) {
        return offerRepo.findByMerchantIdAndActiveTrue(merchantId).stream()
                .map(OfferResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public OfferResponse deactivate(Long offerId, Long userId) {
        Offer offer = offerRepo.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Oferta no encontrada"));

        if (!offer.getMerchant().getUser().getId().equals(userId)) {
            throw new RuntimeException("No tenés permiso para modificar esta oferta");
        }

        offer.setActive(false);
        return OfferResponse.from(offerRepo.save(offer));
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
            code = sb.toString();
        } while (offerRepo.existsByCode(code));
        return code;
    }
}
