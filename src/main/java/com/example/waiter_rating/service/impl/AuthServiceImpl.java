package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.service.AuthService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private final AppUserRepo appUserRepo;
    private final ProfessionalRepo professionalRepository;

    public AuthServiceImpl(AppUserRepo appUserRepo, ProfessionalRepo professionalRepository) {
        this.appUserRepo = appUserRepo;
        this.professionalRepository = professionalRepository;
    }

    @Override
    public Optional<AppUser> getCurrentUser() {
        System.out.println("=== DEBUG getCurrentUser ===");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authentication: " + authentication);

        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("❌ No authenticated");
            return Optional.empty();
        }

        System.out.println("Authentication class: " + authentication.getClass().getName());
        System.out.println("Is authenticated: " + authentication.isAuthenticated());

        Object principal = authentication.getPrincipal();
        System.out.println("Principal class: " + principal.getClass().getName());
        System.out.println("Principal: " + principal);

        // Caso 1: Login con OAuth2 (Google)
        if (principal instanceof OAuth2User) {
            System.out.println("→ OAuth2User detected");
            OAuth2User oAuth2User = (OAuth2User) principal;
            String email = oAuth2User.getAttribute("email");
            System.out.println("Email from OAuth2: " + email);
            Optional<AppUser> user = appUserRepo.findByEmail(email);
            if (user.isEmpty()) {
                // Intentar buscar en ProfessionalRepository
                return professionalRepository.findByEmail(email)
                        .map(p -> (AppUser) p);
            }
            return user;
        }

        // Caso 2: Login con email/password (UserDetails)
        if (principal instanceof UserDetails) {
            System.out.println("→ UserDetails detected");
            String username = ((UserDetails) principal).getUsername();
            System.out.println("Username: " + username);
            Optional<AppUser> user = appUserRepo.findByEmail(username);
            if (user.isEmpty()) {
                // Intentar buscar en ProfessionalRepository
                return professionalRepository.findByEmail(username)
                        .map(p -> (AppUser) p);
            }
            return user;
        }

        // Caso 3: Principal es directamente el email (String)
        if (principal instanceof String) {
            System.out.println("→ String principal detected");
            String email = (String) principal;
            System.out.println("Email: " + email);

            // Primero intentar en AppUserRepo
            Optional<AppUser> user = appUserRepo.findByEmail(email);
            System.out.println("Found in AppUserRepo: " + user.isPresent());

            if (user.isEmpty()) {
                // Si no se encuentra, buscar en ProfessionalRepository
                System.out.println("Trying ProfessionalRepository...");
                Optional<Professional> professional = professionalRepository.findByEmail(email);
                System.out.println("Found in ProfessionalRepository: " + professional.isPresent());

                if (professional.isPresent()) {
                    return Optional.of((AppUser) professional.get());
                }
            }
            return user;
        }

        System.out.println("❌ Unknown principal type");
        return Optional.empty();
    }

    @Override
    public boolean isCurrentUserProfessional() {
        return getCurrentUser()
                .map(user -> user instanceof Professional)
                .orElse(false);
    }

    @Override
    public boolean isCurrentUserClient() {
        return getCurrentUser()
                .map(user -> user instanceof Client)
                .orElse(false);
    }

    @Override
    public Optional<Professional> getCurrentProfessional() {
        System.out.println("=== DEBUG getCurrentProfessional ===");

        // Obtener el usuario actual
        Optional<AppUser> currentUser = getCurrentUser();

        if (currentUser.isEmpty()) {
            System.out.println("❌ No hay usuario autenticado");
            return Optional.empty();
        }

        AppUser user = currentUser.get();
        System.out.println("✅ User found: id=" + user.getId() + ", class=" + user.getClass().getSimpleName());

        // IMPORTANTE: No usar instanceof, buscar directamente en ProfessionalRepository
        // porque Hibernate puede haber cargado como AppUser genérico
        Long userId = user.getId();
        Optional<Professional> professional = professionalRepository.findById(userId);

        if (professional.isEmpty()) {
            System.out.println("❌ No existe Professional con id=" + userId);
        } else {
            System.out.println("✅ Professional found: id=" + professional.get().getId());
        }

        return professional;
    }

    @Override
    public Optional<Client> getCurrentClient() {
        return getCurrentUser()
                .filter(user -> user instanceof Client)
                .map(user -> (Client) user);
    }
}