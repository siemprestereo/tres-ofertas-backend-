package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.request.WorkHistoryRequest;
import com.example.waiter_rating.model.WorkHistory;

import java.time.LocalDate;
import java.util.List;

public interface WorkHistoryService {

    /** Agregar un nuevo trabajo al historial del professional */
    WorkHistory addWorkHistory(Long professionalId, WorkHistoryRequest request);

    /** Actualizar un trabajo existente */
    WorkHistory updateWorkHistory(Long professionalId, Long workHistoryId, WorkHistoryRequest request);

    /** Cerrar un trabajo (poner fecha de fin y marcar como inactivo) */
    WorkHistory closeWorkHistory(Long professionalId, Long workHistoryId, LocalDate endDate);

    /** Eliminar un trabajo del historial */
    void deleteWorkHistory(Long professionalId, Long workHistoryId);

    /** Listar todo el historial laboral de un professional */
    List<WorkHistory> listWorkHistory(Long professionalId);

    /** Listar solo los trabajos activos de un professional */
    List<WorkHistory> listActiveWorkHistory(Long professionalId);

    /** Habilitar trabajo freelance para un professional */
    WorkHistory enableFreelanceWork(Long professionalId);
}