package com.example.waiter_rating.service;

public interface PdfService {

    /**
     * Genera un PDF con el CV completo del profesional
     *
     * @param professionalId ID del profesional
     * @return byte array del PDF generado
     * @throws Exception si ocurre algún error al generar el PDF
     */
    byte[] generateCvPdf(Long professionalId) throws Exception;
}