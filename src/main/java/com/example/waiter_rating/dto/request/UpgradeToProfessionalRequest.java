package com.example.waiter_rating.dto.request;





import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpgradeToProfessionalRequest {

    @NotBlank(message = "El tipo de profesión es requerido")
    private String professionType; // Ej: "WAITER", "ELECTRICIAN", "PAINTER"

    private String professionalTitle; // Ej: "Mozo Profesional", "Electricista Matriculado"
}