package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.service.CvService;
import com.example.waiter_rating.service.RoleSwitchService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class RoleSwitchServiceImpl implements RoleSwitchService {
    private final AppUserRepo appUserRepo;
    private final CvService cvService;

    public RoleSwitchServiceImpl(AppUserRepo appUserRepo, CvService cvService) {
        this.appUserRepo = appUserRepo;
        this.cvService = cvService;
    }

    @Override
    @Transactional
    public AppUser switchRole(Long userId, UserRole newRole, String professionType, String professionalTitle) {
        // 1. Buscar el usuario
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Validar que el nuevo rol sea diferente al actual
        if (user.getActiveRole() == newRole) {
            throw new IllegalStateException("El usuario ya tiene el rol " + newRole);
        }

        // 3. Validar restricción de 6 meses
        if (!user.canSwitchRole()) {
            LocalDateTime nextAllowed = user.getNextAllowedRoleSwitchDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new IllegalStateException(
                    "Solo puedes cambiar de rol una vez cada 6 meses. " +
                            "Podrás cambiar nuevamente el: " + nextAllowed.format(formatter)
            );
        }

        // 4. Configurar campos según el nuevo rol
        if (newRole == UserRole.PROFESSIONAL) {
            // Cambiar a PROFESSIONAL
            if (professionType == null || professionType.isEmpty()) {
                throw new IllegalArgumentException("El tipo de profesión es requerido para convertirse en profesional");
            }

            // Si es la primera vez como Professional, inicializar campos
            if (user.getProfessionType() == null) {
                user.setProfessionType(professionType);
                user.setProfessionalTitle(professionalTitle);
                user.setReputationScore(0.0);
                user.setTotalRatings(0);
                user.setMonthlyWorkplaceChanges(0);
                user.setSearchable(true);
            } else {
                // Si ya fue Professional antes, actualizar profession type si cambió
                user.setProfessionType(professionType);
                if (professionalTitle != null && !professionalTitle.isEmpty()) {
                    user.setProfessionalTitle(professionalTitle);
                }
            }

            System.out.println("📝 Usuario configurado como Professional: " + professionType);

            // Crear CV si no existe
            cvService.getOrCreateForProfessional(userId);
        } else {
            // Cambiar a CLIENT - no requiere campos adicionales
            System.out.println("📝 Usuario configurado como Client");
        }

        // 5. Actualizar el rol activo y la fecha del último cambio
        user.setActiveRole(newRole);
        user.setLastRoleSwitchAt(LocalDateTime.now());
        user = appUserRepo.save(user);

        System.out.println("✅ Usuario " + userId + " cambió de rol a: " + newRole);

        return user;
    }
}