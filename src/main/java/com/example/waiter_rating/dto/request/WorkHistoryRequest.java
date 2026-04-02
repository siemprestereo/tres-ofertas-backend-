package com.example.waiter_rating.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkHistoryRequest {

    private Long businessId; // Opcional: puede ser null si es texto libre

    private String businessName; // ← Quité @NotBlank porque los freelance pueden tener esto vacío

    @NotBlank(message = "El puesto es obligatorio")
    private String position;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate startDate;

    private LocalDate endDate; // null = trabaja actualmente

    private String referenceContact;
    private String referencePhone;

    private String description;

    private Boolean isFreelance = false;

    private Boolean isActive = false; // ← AGREGAR ESTE CAMPO
}