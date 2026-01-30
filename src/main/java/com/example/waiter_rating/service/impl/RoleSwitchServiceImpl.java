package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.ProfessionType;
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
    private final ClientRepo clientRepo;
    private final ProfessionalRepo professionalRepo;
    private final CvService cvService;

    public RoleSwitchServiceImpl(AppUserRepo appUserRepo, ClientRepo clientRepo, ProfessionalRepo professionalRepo, CvService cvService) {
        this.appUserRepo = appUserRepo;
        this.clientRepo = clientRepo;
        this.professionalRepo = professionalRepo;
        this.cvService = cvService;
    }

    @Override
    @Transactional
    public AppUser switchRole(Long userId, AppUser.UserRole newRole, String professionType, String professionalTitle) {
        // 1. Buscar el usuario base
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // 2. Validar que el nuevo rol sea diferente al actual
        if (user.getActiveRole() == newRole) {
            throw new IllegalStateException("El usuario ya tiene el rol " + newRole);
        }

        // 3. ✅ VALIDACIÓN: Verificar restricción de 6 meses
        if (!user.canSwitchRole()) {
            LocalDateTime nextAllowed = user.getNextAllowedRoleSwitchDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            throw new IllegalStateException(
                    "Solo puedes cambiar de rol una vez cada 6 meses. " +
                            "Podrás cambiar nuevamente el: " + nextAllowed.format(formatter)
            );
        }

        // 4. Según el nuevo rol, asegurar que exista el perfil correspondiente
        if (newRole == AppUser.UserRole.PROFESSIONAL) {
            // Cambiar a PROFESSIONAL
            ensureProfessionalExists(user, professionType, professionalTitle);
        } else {
            // Cambiar a CLIENT
            ensureClientExists(user);
        }

        // 5. Actualizar el rol activo y la fecha del último cambio
        user.setActiveRole(newRole);
        user.setLastRoleSwitchAt(LocalDateTime.now()); // ← NUEVO: Actualizar fecha
        user = appUserRepo.save(user);

        System.out.println("✅ Usuario " + userId + " cambió de rol a: " + newRole);

        return user;
    }

    /**
     * Asegura que el usuario tenga un perfil de Professional
     */
    private void ensureProfessionalExists(AppUser user, String professionType, String professionalTitle) {
        Professional existing = professionalRepo.findByEmail(user.getEmail()).orElse(null);

        if (existing == null) {
            System.out.println("📝 Creando perfil de Professional para: " + user.getEmail());

            if (professionType == null || professionType.isEmpty()) {
                throw new IllegalArgumentException("El tipo de profesión es requerido para convertirse en profesional");
            }

            // Validar que el professionType sea válido
            ProfessionType profession;
            try {
                profession = ProfessionType.valueOf(professionType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de profesión inválido: " + professionType);
            }

            Professional professional = new Professional();
            professional.setEmail(user.getEmail());
            professional.setName(user.getName());
            professional.setPassword(user.getPassword());
            professional.setProfilePicture(user.getProfilePicture());
            professional.setEmailVerified(user.getEmailVerified());
            professional.setProvider(user.getProvider());
            professional.setProviderId(user.getProviderId());
            professional.setPhone(user.getPhone());
            professional.setLocation(user.getLocation());
            professional.setProfessionType(profession);
            professional.setProfessionalTitle(professionalTitle);
            professional.setActiveRole(AppUser.UserRole.PROFESSIONAL);

            professional = professionalRepo.save(professional);

            cvService.getOrCreateForProfessional(professional.getId());

            System.out.println("✅ Perfil de Professional creado con ID: " + professional.getId());
        } else {
            System.out.println("ℹ️ El usuario ya tenía perfil de Professional (ID: " + existing.getId() + ")");
        }
    }

    /**
     * Asegura que el usuario tenga un perfil de Client
     */
    private void ensureClientExists(AppUser user) {
        // Buscar si ya existe como Client
        Client existing = clientRepo.findByEmail(user.getEmail()).orElse(null);

        if (existing == null) {
            // Crear nuevo Client
            System.out.println("📝 Creando perfil de Client para: " + user.getEmail());

            Client client = new Client();
            client.setEmail(user.getEmail());
            client.setName(user.getName());
            client.setPassword(user.getPassword());
            client.setProfilePicture(user.getProfilePicture());
            client.setEmailVerified(user.getEmailVerified());
            client.setProvider(user.getProvider());
            client.setProviderId(user.getProviderId());
            client.setPhone(user.getPhone());
            client.setLocation(user.getLocation());
            client.setActiveRole(AppUser.UserRole.CLIENT);

            client = clientRepo.save(client);

            System.out.println("✅ Perfil de Client creado con ID: " + client.getId());
        } else {
            System.out.println("ℹ️ El usuario ya tenía perfil de Client (ID: " + existing.getId() + ")");
        }
    }
}