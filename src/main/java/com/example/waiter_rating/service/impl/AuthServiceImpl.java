package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;
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

    public AuthServiceImpl(AppUserRepo appUserRepo) {
        this.appUserRepo = appUserRepo;
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

        String email = null;

        // Caso 1: Login con OAuth2 (Google)
        if (principal instanceof OAuth2User) {
            System.out.println("→ OAuth2User detected");
            OAuth2User oAuth2User = (OAuth2User) principal;
            email = oAuth2User.getAttribute("email");
            System.out.println("Email from OAuth2: " + email);
        }
        // Caso 2: Login con email/password (UserDetails)
        else if (principal instanceof UserDetails) {
            System.out.println("→ UserDetails detected");
            email = ((UserDetails) principal).getUsername();
            System.out.println("Username: " + email);
        }
        // Caso 3: Principal es directamente el email (String)
        else if (principal instanceof String) {
            System.out.println("→ String principal detected");
            email = (String) principal;
            System.out.println("Email: " + email);
        }

        if (email != null) {
            Optional<AppUser> user = appUserRepo.findByEmail(email);
            System.out.println("Found user: " + user.isPresent());
            return user;
        }

        System.out.println("❌ Unknown principal type");
        return Optional.empty();
    }

    @Override
    public boolean isCurrentUserProfessional() {
        return getCurrentUser()
                .map(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElse(false);
    }

    @Override
    public boolean isCurrentUserClient() {
        return getCurrentUser()
                .map(user -> UserRole.CLIENT.equals(user.getActiveRole()))
                .orElse(false);
    }

    @Override
    public Optional<AppUser> getCurrentProfessional() {
        System.out.println("=== DEBUG getCurrentProfessional ===");

        Optional<AppUser> currentUser = getCurrentUser();

        if (currentUser.isEmpty()) {
            System.out.println("❌ No hay usuario autenticado");
            return Optional.empty();
        }

        AppUser user = currentUser.get();
        System.out.println("✅ User found: id=" + user.getId() + ", activeRole=" + user.getActiveRole());

        // Verificar que el activeRole sea PROFESSIONAL
        if (UserRole.PROFESSIONAL.equals(user.getActiveRole())) {
            System.out.println("✅ User is a Professional");
            return Optional.of(user);
        }

        System.out.println("❌ User is not a Professional (activeRole=" + user.getActiveRole() + ")");
        return Optional.empty();
    }

    @Override
    public Optional<AppUser> getCurrentClient() {
        return getCurrentUser()
                .filter(user -> UserRole.CLIENT.equals(user.getActiveRole()))
                .map(Optional::of)
                .orElse(Optional.empty());
    }
}
