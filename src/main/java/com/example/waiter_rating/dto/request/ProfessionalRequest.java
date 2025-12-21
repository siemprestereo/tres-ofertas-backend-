package com.example.waiter_rating.dto.request;

import com.example.waiter_rating.model.ProfessionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "El tipo de profesión es obligatorio")
    private ProfessionType professionType;

    private String profilePicture;

    private String provider; // "GOOGLE" o "LOCAL"

    private String providerId; // ID de Google OAuth
}