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
import java.util.HashSet;
import java.util.List;

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
    public AppUser switchRole(Long userId, UserRole newRole, List<String> professionTypes, String professionalTitle) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getActiveRole() == newRole) {
            throw new IllegalStateException("El usuario ya tiene el rol " + newRole);
        }

        if (!user.canSwitchRole()) {
            LocalDateTime nextAllowed = user.getNextAllowedRoleSwitchDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new IllegalStateException(
                    "Solo puedes cambiar de rol una vez cada 6 meses. " +
                            "Podrás cambiar nuevamente el: " + nextAllowed.format(formatter)
            );
        }

        if (newRole == UserRole.PROFESSIONAL) {
            if (professionTypes == null || professionTypes.isEmpty()) {
                throw new IllegalArgumentException("Al menos una profesión es requerida para convertirse en profesional");
            }

            user.getProfessionTypes().clear();
            user.getProfessionTypes().addAll(professionTypes);

            // legacy: guardar la primera profesión en professionType
            user.setProfessionType(professionTypes.get(0));

            if (user.getReputationScore() == null) user.setReputationScore(0.0);
            if (user.getTotalRatings() == null) user.setTotalRatings(0);
            if (user.getMonthlyWorkplaceChanges() == null) user.setMonthlyWorkplaceChanges(0);
            if (user.getSearchable() == null || !user.getSearchable()) user.setSearchable(true);

            if (professionalTitle != null && !professionalTitle.isEmpty()) {
                user.setProfessionalTitle(professionalTitle);
            }

            cvService.getOrCreateForProfessional(userId);
        }

        user.setActiveRole(newRole);
        user.setLastRoleSwitchAt(LocalDateTime.now());
        return appUserRepo.save(user);
    }
}
