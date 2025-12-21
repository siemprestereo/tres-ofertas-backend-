package com.example.waiter_rating.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    private String password; // Para registro local (opcional si usa Google)

    private String profilePicture;

    private String provider; // "GOOGLE" o "LOCAL"

    private String providerId; // ID de Google OAuth
}