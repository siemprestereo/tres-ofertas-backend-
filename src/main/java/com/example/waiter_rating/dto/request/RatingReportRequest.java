package com.example.waiter_rating.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RatingReportRequest {

    @NotBlank(message = "El motivo de la denuncia es obligatorio")
    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String reason;
}