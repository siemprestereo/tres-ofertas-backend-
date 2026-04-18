package com.tresofertas.service;

import com.tresofertas.dto.request.CreateOfferRequest;
import com.tresofertas.dto.response.OfferResponse;

import java.util.List;

public interface OfferService {
    OfferResponse create(Long merchantId, CreateOfferRequest request, Long userId);
    List<OfferResponse> getByMerchant(Long merchantId);
    OfferResponse deactivate(Long offerId, Long userId);
}
