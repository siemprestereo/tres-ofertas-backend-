package com.example.waiter_rating.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter @Setter
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;          // "Bad Request", "Conflict", etc.
    private String message;        // mensaje principal
    private String path;           // endpoint
    private Map<String, String> fieldErrors; // validaciones @Valid (campo -> error)
}

