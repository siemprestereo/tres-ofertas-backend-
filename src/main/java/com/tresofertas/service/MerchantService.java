package com.tresofertas.service;

import com.tresofertas.dto.request.CreateMerchantRequest;
import com.tresofertas.dto.response.MerchantResponse;

import java.util.List;

public interface MerchantService {
    MerchantResponse create(CreateMerchantRequest request, Long userId);
    MerchantResponse getBySlug(String slug);
    List<MerchantResponse> getMyMerchants(Long userId);
    MerchantResponse update(Long merchantId, CreateMerchantRequest request, Long userId);
}
