package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.WorkHistoryRequest;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.Business;
import com.example.waiter_rating.model.BusinessType;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.model.WorkHistory;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.BusinessRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.repository.WorkHistoryRepo;
import com.example.waiter_rating.service.WorkHistoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class WorkHistoryServiceImpl implements WorkHistoryService {

    private final WorkHistoryRepo workHistoryRepo;
    private final AppUserRepo appUserRepo;
    private final BusinessRepo businessRepo;
    private final RatingRepo ratingRepo;

    public WorkHistoryServiceImpl(WorkHistoryRepo workHistoryRepo,
                                  AppUserRepo appUserRepo,
                                  BusinessRepo businessRepo,
                                  RatingRepo ratingRepo) {
        this.workHistoryRepo = workHistoryRepo;
        this.appUserRepo = appUserRepo;
        this.businessRepo = businessRepo;
        this.ratingRepo = ratingRepo;
    }

    @Override
    @Transactional
    public WorkHistory addWorkHistory(Long professionalId, WorkHistoryRequest request) {
        AppUser professional = appUserRepo.findById(professionalId)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + professionalId));

        boolean isActiveJob = (request.getEndDate() == null);

        // Validar límite de 3 trabajos activos simultáneos
        if (isActiveJob) {
            validateMaxActiveJobs(professionalId);
            // NO validar cambios mensuales aquí - esto es un trabajo NUEVO, no un cambio
        }

        Business business;
        if (request.getBusinessId() != null) {
            business = businessRepo.findById(request.getBusinessId())
                    .orElseThrow(() -> new IllegalArgumentException("Business no encontrado: " + request.getBusinessId()));
        } else {
            business = Business.builder()
                    .name(request.getBusinessName())
                    .businessType(BusinessType.RESTAURANT)
                    .build();
            business = businessRepo.save(business);
        }

        WorkHistory workHistory = WorkHistory.builder()
                .professional(professional)
                .business(business)
                .businessName(request.getBusinessName())
                .position(request.getPosition())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(isActiveJob)
                .isFreelance(request.getIsFreelance() != null ? request.getIsFreelance() : false)
                .referenceContact(request.getReferenceContact())
                .referencePhone(request.getReferencePhone())
                .description(request.getDescription())
                .build();

        workHistory = workHistoryRepo.save(workHistory);

        // NO registrar cambio de lugar de trabajo al crear uno nuevo
        // Solo se registra cuando CAMBIAS un trabajo existente de inactivo a activo

        return workHistory;
    }

    private void registerWorkplaceChange(AppUser professional) {
        LocalDate now = LocalDate.now();
        LocalDate lastChange = professional.getLastWorkplaceChangeDate();

        if (lastChange == null || YearMonth.from(lastChange).isBefore(YearMonth.from(now))) {
            professional.setMonthlyWorkplaceChanges(1);
        } else {
            professional.setMonthlyWorkplaceChanges(professional.getMonthlyWorkplaceChanges() + 1);
        }

        professional.setLastWorkplaceChangeDate(now);
    }

    @Override
    public WorkHistory updateWorkHistory(Long professionalId, Long workHistoryId, WorkHistoryRequest request) {
        WorkHistory workHistory = workHistoryRepo.findById(workHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("Trabajo no encontrado con ID: " + workHistoryId));

        if (!workHistory.getProfessional().getId().equals(professionalId)) {
            throw new IllegalArgumentException("Este trabajo no pertenece al profesional especificado");
        }

        long ratingCount = ratingRepo.countByWorkHistoryId(workHistoryId);

        if (ratingCount > 0) {
            if (request.getPosition() != null && !request.getPosition().equals(workHistory.getPosition())) {
                throw new IllegalStateException(
                        "No se puede cambiar el puesto de un trabajo que tiene " +
                                ratingCount + " calificación" + (ratingCount > 1 ? "es" : "")
                );
            }

            if (request.getBusinessName() != null && !request.getBusinessName().equals(workHistory.getBusinessName())) {
                throw new IllegalStateException(
                        "No se puede cambiar la empresa de un trabajo que tiene " +
                                ratingCount + " calificación" + (ratingCount > 1 ? "es" : "")
                );
            }

            if (request.getIsFreelance() != null && !request.getIsFreelance().equals(workHistory.getIsFreelance())) {
                throw new IllegalStateException(
                        "No se puede cambiar el tipo de trabajo (autónomo/relación de dependencia) " +
                                "de un trabajo que tiene " + ratingCount + " calificación" + (ratingCount > 1 ? "es" : "")
                );
            }
        }

        if (request.getDescription() != null) {
            workHistory.setDescription(request.getDescription());
        }
        if (request.getReferenceContact() != null) {
            workHistory.setReferenceContact(request.getReferenceContact());
        }
        if (request.getReferencePhone() != null) {
            workHistory.setReferencePhone(request.getReferencePhone());
        }
        if (request.getStartDate() != null) {
            workHistory.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            workHistory.setEndDate(request.getEndDate());
        }

        if (ratingCount == 0) {
            if (request.getPosition() != null) {
                workHistory.setPosition(request.getPosition());
            }
            if (request.getBusinessName() != null) {
                workHistory.setBusinessName(request.getBusinessName());
            }
            if (request.getIsFreelance() != null) {
                workHistory.setIsFreelance(request.getIsFreelance());
            }
        }

        if (request.getEndDate() != null) {
            workHistory.setIsActive(false);
        } else {
            workHistory.setIsActive(true);
        }

        return workHistoryRepo.save(workHistory);
    }

    @Override
    @Transactional
    public WorkHistory closeWorkHistory(Long professionalId, Long workHistoryId, LocalDate endDate) {
        WorkHistory workHistory = workHistoryRepo.findById(workHistoryId)
                .orElseThrow(() -> new IllegalArgumentException("WorkHistory no encontrado: " + workHistoryId));

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
        AppUser professional = appUserRepo.findById(professionalId)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + professionalId));

        Optional<WorkHistory> existingFreelance = workHistoryRepo.findByProfessionalIdAndIsActiveTrue(professionalId)
                .stream()
                .filter(wh -> wh.getBusiness() != null && wh.getBusiness().getBusinessType() == BusinessType.FREELANCE)
                .findFirst();

        if (existingFreelance.isPresent()) {
            throw new IllegalStateException("El professional ya tiene trabajo independiente activo");
        }

        String freelanceName = professional.getName() + " - Independiente";
        Business freelanceBusiness = businessRepo.findByNameIgnoreCase(freelanceName)
                .orElseGet(() -> {
                    Business newBusiness = Business.builder()
                            .name(freelanceName)
                            .businessType(BusinessType.FREELANCE)
                            .build();
                    return businessRepo.save(newBusiness);
                });

        WorkHistory workHistory = WorkHistory.builder()
                .professional(professional)
                .business(freelanceBusiness)
                .businessName("Independiente")
                .position("Trabajo Autónomo")
                .startDate(LocalDate.now())
                .isActive(true)
                .isFreelance(true)
                .build();

        return workHistoryRepo.save(workHistory);
    }

    @Override
    @Transactional
    public void disableFreelanceWork(Long professionalId) {
        AppUser professional = appUserRepo.findById(professionalId)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + professionalId));

        Optional<WorkHistory> freelanceWork = workHistoryRepo.findByProfessionalIdAndIsActiveTrue(professionalId)
                .stream()
                .filter(wh -> wh.getBusiness() != null && wh.getBusiness().getBusinessType() == BusinessType.FREELANCE)
                .findFirst();

        if (freelanceWork.isEmpty()) {
            throw new IllegalStateException("No se encontró trabajo independiente activo para desactivar");
        }

        WorkHistory work = freelanceWork.get();
        work.closeJob(LocalDate.now());
        workHistoryRepo.save(work);
    }

    private void validateMaxActiveJobs(Long professionalId) {
        List<WorkHistory> activeJobs = workHistoryRepo.findByProfessionalIdAndIsActiveTrue(professionalId);

        if (activeJobs.size() >= 3) {
            throw new IllegalStateException(
                    "Ya tenés 3 trabajos activos. Marcá uno como finalizado antes de agregar otro trabajo actual."
            );
        }
    }

    private void validateMonthlyWorkplaceChanges(AppUser professional) {
        if (professional.getLastWorkplaceChangeDate() == null) {
            return;
        }

        YearMonth lastChangeMonth = YearMonth.from(professional.getLastWorkplaceChangeDate());
        YearMonth currentMonth = YearMonth.now();

        if (!lastChangeMonth.equals(currentMonth)) {
            return;
        }

        if (professional.getMonthlyWorkplaceChanges() >= 2) {
            throw new IllegalStateException(
                    "Has alcanzado el límite de 3 cambios de trabajo activo por mes. " +
                            "Podrás agregar más a partir del próximo mes. " +
                            "Nota: Podés seguir agregando trabajos anteriores (finalizados) sin problema."
            );
        }
    }

    @Override
    public long countActiveJobsByProfessional(Long professionalId) {
        return workHistoryRepo.findByProfessionalId(professionalId)
                .stream()
                .filter(wh -> wh.getIsActive() != null && wh.getIsActive())
                .count();
    }

    @Override
    public WorkHistory getById(Long workHistoryId) {
        return workHistoryRepo.findById(workHistoryId)
                .orElseThrow(() -> new RuntimeException("WorkHistory no encontrado con ID: " + workHistoryId));
    }
}