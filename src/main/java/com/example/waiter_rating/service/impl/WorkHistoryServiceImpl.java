package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.WorkHistoryRequest;
import com.example.waiter_rating.model.Business;
import com.example.waiter_rating.model.BusinessType;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.model.WorkHistory;
import com.example.waiter_rating.repository.BusinessRepo;
import com.example.waiter_rating.repository.ProfessionalRepo;
import com.example.waiter_rating.repository.WorkHistoryRepo;
import com.example.waiter_rating.service.WorkHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class WorkHistoryServiceImpl implements WorkHistoryService {

    private final WorkHistoryRepo workHistoryRepo;
    private final ProfessionalRepo professionalRepo;
    private final BusinessRepo businessRepo;

    public WorkHistoryServiceImpl(WorkHistoryRepo workHistoryRepo,
                                  ProfessionalRepo professionalRepo,
                                  BusinessRepo businessRepo) {
        this.workHistoryRepo = workHistoryRepo;
        this.professionalRepo = professionalRepo;
        this.businessRepo = businessRepo;
    }

    @Override
    @Transactional
    public WorkHistory addWorkHistory(Long professionalId, WorkHistoryRequest request) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + professionalId));

        // Determinar si es trabajo activo
        boolean isActiveJob = (request.getEndDate() == null);

        // Validar límites SOLO si es trabajo ACTIVO
        if (isActiveJob) {
            // Validar máximo 3 trabajos activos simultáneos
            validateMaxActiveJobs(professionalId);

            // Validar límite mensual de cambios
            validateMonthlyWorkplaceChanges(professional);
        }

        Business business = null;
        if (request.getBusinessId() != null) {
            business = businessRepo.findById(request.getBusinessId())
                    .orElseThrow(() -> new IllegalArgumentException("Business no encontrado: " + request.getBusinessId()));
        }

        WorkHistory workHistory = WorkHistory.builder()
                .professional(professional)
                .business(business)
                .businessName(request.getBusinessName())
                .position(request.getPosition())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(isActiveJob)
                .referenceContact(request.getReferenceContact())
                .build();

        workHistory = workHistoryRepo.save(workHistory);

        // Registrar cambio de workplace SOLO si es trabajo activo
        if (isActiveJob) {
            professional.registerWorkplaceChange();
            professionalRepo.save(professional);
        }

        return workHistory;
    }

    @Override
    @Transactional
    public WorkHistory updateWorkHistory(Long professionalId, Long workHistoryId, WorkHistoryRequest request) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + professionalId));

        WorkHistory workHistory = workHistoryRepo.findById(workHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("WorkHistory no encontrado: " + workHistoryId));

        // Verificar que el WorkHistory pertenece al professional
        if (!workHistory.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("Este trabajo no pertenece al professional especificado");
        }

        boolean wasActive = workHistory.getIsActive();
        boolean willBeActive = (request.getEndDate() == null);

        // Si está cambiando de inactivo a activo, validar límites
        if (!wasActive && willBeActive) {
            validateMaxActiveJobs(professionalId);
            validateMonthlyWorkplaceChanges(professional);

            professional.registerWorkplaceChange();
            professionalRepo.save(professional);
        }

        // Actualizar business si cambió
        if (request.getBusinessId() != null) {
            Business business = businessRepo.findById(request.getBusinessId())
                    .orElseThrow(() -> new IllegalArgumentException("Business no encontrado: " + request.getBusinessId()));
            workHistory.setBusiness(business);
        }

        workHistory.setBusinessName(request.getBusinessName());
        workHistory.setPosition(request.getPosition());
        workHistory.setStartDate(request.getStartDate());
        workHistory.setEndDate(request.getEndDate());
        workHistory.setIsActive(willBeActive);
        workHistory.setReferenceContact(request.getReferenceContact());

        return workHistoryRepo.save(workHistory);
    }

    @Override
    @Transactional
    public WorkHistory closeWorkHistory(Long professionalId, Long workHistoryId, LocalDate endDate) {
        WorkHistory workHistory = workHistoryRepo.findById(workHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("WorkHistory no encontrado: " + workHistoryId));

        // Verificar que el WorkHistory pertenece al professional
        if (!workHistory.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("Este trabajo no pertenece al professional especificado");
        }

        workHistory.closeJob(endDate);
        return workHistoryRepo.save(workHistory);
    }

    @Override
    @Transactional
    public void deleteWorkHistory(Long professionalId, Long workHistoryId) {
        WorkHistory workHistory = workHistoryRepo.findById(workHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("WorkHistory no encontrado: " + workHistoryId));

        // Verificar que el WorkHistory pertenece al professional
        if (!workHistory.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("Este trabajo no pertenece al professional especificado");
        }

        workHistoryRepo.delete(workHistory);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkHistory> listWorkHistory(Long professionalId) {
        return workHistoryRepo.findByProfessionalId(professionalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkHistory> listActiveWorkHistory(Long professionalId) {
        return workHistoryRepo.findByProfessionalIdAndIsActiveTrue(professionalId);
    }

    @Override
    @Transactional
    public WorkHistory enableFreelanceWork(Long professionalId) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + professionalId));

        // Verificar si ya tiene freelance activo
        boolean hasFreelance = workHistoryRepo.findByProfessionalIdAndIsActiveTrue(professionalId)
                .stream()
                .anyMatch(wh -> wh.getBusiness() != null && wh.getBusiness().getBusinessType() == BusinessType.FREELANCE);

        if (hasFreelance) {
            throw new IllegalStateException("El professional ya tiene un trabajo freelance activo");
        }

        // Crear Business personal
        Business freelanceBusiness = Business.builder()
                .name(professional.getName() + " - Independiente")
                .businessType(BusinessType.FREELANCE)
                .build();
        freelanceBusiness = businessRepo.save(freelanceBusiness);

        // Crear WorkHistory
        WorkHistory workHistory = WorkHistory.builder()
                .professional(professional)
                .business(freelanceBusiness)
                .position("Independiente")
                .startDate(LocalDate.now())
                .isActive(true)
                .build();

        return workHistoryRepo.save(workHistory);
    }

    /**
     * Valida que el professional no tenga más de 3 trabajos activos simultáneos
     */
    private void validateMaxActiveJobs(Long professionalId) {
        List<WorkHistory> activeJobs = workHistoryRepo.findByProfessionalIdAndIsActiveTrue(professionalId);

        if (activeJobs.size() >= 3) {
            throw new IllegalStateException(
                    "Ya tenés 3 trabajos activos. Marcá uno como finalizado antes de agregar otro trabajo actual."
            );
        }
    }

    /**
     * Valida que el professional no haya excedido el límite de cambios mensuales
     * Este límite solo aplica a trabajos ACTIVOS
     */
    private void validateMonthlyWorkplaceChanges(Professional professional) {
        if (!professional.canChangeWorkplace()) {
            throw new IllegalStateException(
                    "Has alcanzado el límite de 3 cambios de trabajo activo por mes. " +
                            "Podrás agregar más a partir del próximo mes. " +
                            "Nota: Podés seguir agregando trabajos anteriores (finalizados) sin problema."
            );
        }
    }
}