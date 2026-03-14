package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.response.AdminStatsResponse;
import com.example.waiter_rating.dto.response.AppUserResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.PasswordResetToken;
import com.example.waiter_rating.model.VerificationToken;
import com.example.waiter_rating.model.enums.AuthProvider;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.PasswordResetTokenRepository;
import com.example.waiter_rating.repository.VerificationTokenRepository;
import com.example.waiter_rating.service.AppUserService;
import com.example.waiter_rating.service.EmailService;
import com.example.waiter_rating.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.waiter_rating.dto.response.AdminUserResponse;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepo repo;
    private final JwtService jwtService;

    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AppUserServiceImpl(AppUserRepo repo, JwtService jwtService, VerificationTokenRepository verificationTokenRepository, PasswordResetTokenRepository passwordResetTokenRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.jwtService = jwtService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AppUserResponse getById(Long id) {
        AppUser user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));

        return mapToResponse(user);
    }

    @Override
    public List<AppUserResponse> listAll() {
        List<AppUser> users = repo.findAll();
        return users.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> checkUserRoles(String authHeader) {
        try {
            System.out.println("=== checkUserRoles START ===");

            String token = authHeader.substring(7);
            Claims claims = jwtService.validateToken(token);
            String email = claims.getSubject();
            System.out.println("Email del token: " + email);

            AppUser user = repo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Con AppUser unificado, un usuario puede tener AMBOS roles configurados
            // Solo necesitamos verificar si tiene los campos de Professional poblados
            boolean hasClientRole = true; // Todos los usuarios pueden ser clientes
            boolean hasProfessionalRole = user.getProfessionType() != null; // Solo si tiene profession type configurado

            System.out.println("hasClientRole: " + hasClientRole);
            System.out.println("hasProfessionalRole: " + hasProfessionalRole);
            System.out.println("activeRole: " + user.getActiveRole());

            Map<String, Object> response = new HashMap<>();
            response.put("hasClientRole", hasClientRole);
            response.put("hasProfessionalRole", hasProfessionalRole);
            response.put("activeRole", user.getActiveRole().name());
            response.put("canSwitchRole", user.canSwitchRole());

            if (!user.canSwitchRole()) {
                response.put("nextAllowedSwitchDate", user.getNextAllowedRoleSwitchDate());
            }

            System.out.println("Response: " + response);
            System.out.println("=== checkUserRoles SUCCESS ===");
            return response;
        } catch (Exception e) {
            System.err.println("=== checkUserRoles ERROR ===");
            e.printStackTrace();
            throw new RuntimeException("Error verificando roles: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createVerificationToken(AppUser user) {
        // Eliminar tokens anteriores
        verificationTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);
        log.info("Verification token created for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("Verification token not found: {}", token);
            return false;
        }

        VerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.isExpired()) {
            log.warn("Verification token expired: {}", token);
            return false;
        }

        if (verificationToken.isUsed()) {
            log.warn("Verification token already used: {}", token);
            return false;
        }

        AppUser user = verificationToken.getUser();
        user.setEmailVerified(true);
        repo.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
        return true;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<AppUser> userOpt = repo.findByEmailAndAuthProvider(email, AuthProvider.LOCAL);

        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent or OAuth user: {}", email);
            // Por seguridad, no revelamos si el email existe
            return;
        }

        AppUser user = userOpt.get();

        // Eliminar tokens anteriores
        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        log.info("Password reset email sent to: {}", email);
    }

    @Override
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("Password reset token not found: {}", token);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            log.warn("Password reset token expired: {}", token);
            return false;
        }

        if (resetToken.isUsed()) {
            log.warn("Password reset token already used: {}", token);
            return false;
        }

        AppUser user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        repo.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset successfully for user: {}", user.getEmail());
        return true;
    }

    @Override
    public void sendWelcomeEmail(AppUser user) {
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
        log.info("Welcome email sent to: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void updateProfilePicture(Long userId, String photoUrl) {
        AppUser user = repo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + userId));
        user.setProfilePicture(photoUrl);
        repo.save(user);
        log.info("Profile picture updated for user id: {}", userId);
    }

    @Override
    public List<AdminUserResponse> listAllForAdmin() {
        return repo.findAll().stream()
                .map(u -> new AdminUserResponse(
                        u.getId(),
                        u.getName(),
                        u.getEmail(),
                        u.getActiveRole().name(),
                        u.getSuspended(),
                        u.getEmailVerified(),
                        u.getAuthProvider().name(),
                        u.getCreatedAt(),
                        u.getTotalRatings()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void toggleSuspend(Long id) {
        AppUser user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        user.setSuspended(!user.getSuspended());
        repo.save(user);
        log.info("Usuario {} -> suspended: {}", id, user.getSuspended());
    }

    @Override
    public AdminStatsResponse getAdminStats() {
        List<AppUser> all = repo.findAll();
        return new AdminStatsResponse(
                (long) all.size(),
                all.stream().filter(AppUser::isProfessional).count(),
                all.stream().filter(AppUser::isClient).count(),
                all.stream().filter(AppUser::getSuspended).count(),
                all.stream().mapToLong(u -> u.getTotalRatings()).sum(),
                all.stream().filter(AppUser::isProfessional)
                        .mapToDouble(AppUser::getReputationScore)
                        .average()
                        .orElse(0.0)
        );
    }

    private AppUserResponse mapToResponse(AppUser user) {
        AppUserResponse response = new AppUserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setLocation(user.getLocation());
        response.setProfessionalTitle(user.getProfessionalTitle());
        response.setUserType(user.getUserType());
        response.setActiveRole(user.getActiveRole().name());
        response.setProfilePicture(user.getProfilePicture());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}