package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.request.PdfGenerationRequest;

public interface PdfService {

    byte[] generateCvPdf(Long professionalId) throws Exception;

    byte[] generateCvPdf(Long professionalId, PdfGenerationRequest request) throws Exception;
}