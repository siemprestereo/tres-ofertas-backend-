package com.example.waiter_rating.dto.request;

import com.example.waiter_rating.model.ProfessionType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppUserRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "El tipo de usuario es obligatorio (CLIENT o PROFESSIONAL)")
    private String userType; // "CLIENT" o "PROFESSIONAL"

    private ProfessionType professionType; // Solo si userType = PROFESSIONAL

    private String profilePicture;
}