package com.example.waiter_rating.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfessionalRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El tipo de profesión es obligatorio")
    private String professionType;

    private String profilePicture;

    private String provider; // "GOOGLE" o "LOCAL"

    private String providerId; // ID de Google OAuth
}