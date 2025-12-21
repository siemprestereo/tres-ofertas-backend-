package com.example.waiter_rating.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
@Getter @Setter
public class CvDescriptionRequest {


        @Size(max = 500, message = "La descripción no puede superar 500 caracteres")
        private String description; // puede ser null para limpiar
    }


