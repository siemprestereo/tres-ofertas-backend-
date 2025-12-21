package com.example.waiter_rating.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RatingFromQrRequest {

        @NotNull(message = "businessId es obligatorio")
        private Long businessId;

        @NotNull(message = "score es obligatorio")
        @Min(value = 1, message = "El puntaje mínimo es 1")
        @Max(value = 5, message = "El puntaje máximo es 5")
        private Integer score;

        @Size(max = 140, message = "El comentario no puede superar 140 caracteres")
        private String comment;

        // Por ahora opcional. Más adelante vendrá del usuario logueado.
        private Long clientId;
}