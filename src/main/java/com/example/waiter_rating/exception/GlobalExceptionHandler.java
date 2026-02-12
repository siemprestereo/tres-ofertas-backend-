package com.example.waiter_rating.exception;

import com.example.waiter_rating.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Logger para que puedas ver los errores reales en la consola de Railway
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Captura errores de validación (ej: cuando un campo @NotNull llega vacío).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        log.warn("Error de validación en {}: {}", req.getRequestURI(), fieldErrors);

        return build(HttpStatus.BAD_REQUEST,
                "Error de Validación",
                "Los datos enviados no son correctos.",
                req.getRequestURI(),
                fieldErrors);
    }

    /**
     * Captura errores de lógica de negocio (Argumentos incorrectos).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest req) {

        log.warn("Solicitud incorrecta (400) en {}: {}", req.getRequestURI(), ex.getMessage());

        return build(HttpStatus.BAD_REQUEST,
                "Solicitud Incorrecta",
                ex.getMessage(),
                req.getRequestURI(),
                null);
    }

    /**
     * Captura conflictos de estado (Ej: intentar realizar una acción no permitida).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            IllegalStateException ex, HttpServletRequest req) {

        log.warn("Conflicto de estado (409) en {}: {}", req.getRequestURI(), ex.getMessage());

        return build(HttpStatus.CONFLICT,
                "Conflicto",
                ex.getMessage(),
                req.getRequestURI(),
                null);
    }

    /**
     * Captura errores de base de datos (Ej: un email duplicado).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(
            DataIntegrityViolationException ex, HttpServletRequest req) {

        // Logueamos el error técnico para nosotros
        log.error("Violación de integridad en base de datos: {}", ex.getMessage());

        // Mensaje genérico para el usuario por seguridad
        return build(HttpStatus.CONFLICT,
                "Conflicto de Datos",
                "No se pudo guardar la información. Es posible que el registro ya exista o falten datos vinculados.",
                req.getRequestURI(),
                null);
    }

    /**
     * EL ESCUDO FINAL: Captura cualquier error inesperado (NullPointerException, etc).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {

        // Crítico: Esto guarda el error completo (Stacktrace) en tus logs privados
        log.error("¡ERROR INTERNO CRÍTICO! en " + req.getRequestURI(), ex);

        // Al usuario le ocultamos TODA la información técnica
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error Interno del Servidor",
                "Ocurrió un problema inesperado. Nuestro equipo técnico ha sido notificado.",
                req.getRequestURI(),
                null);
    }

    /**
     * Método auxiliar para estandarizar la respuesta de error.
     */
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, String path, Map<String, String> fieldErrors) {

        ErrorResponse body = new ErrorResponse();
        body.setTimestamp(LocalDateTime.now());
        body.setStatus(status.value());
        body.setError(error);
        body.setMessage(message);
        body.setPath(path);
        body.setFieldErrors(fieldErrors);

        return ResponseEntity.status(status).body(body);
    }
}