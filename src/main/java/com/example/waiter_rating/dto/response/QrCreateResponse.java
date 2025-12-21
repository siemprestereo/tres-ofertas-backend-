package com.example.waiter_rating.dto.response;



import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class QrCreateResponse {
    private String code;          // código único del token
    private String deepLink;      // URL que irá dentro del QR (ej: /qr/{code})
    private String expiresAt;     // ISO-8601
    private String qrPngBase64;   // opcional (si generás imagen del QR)
}

