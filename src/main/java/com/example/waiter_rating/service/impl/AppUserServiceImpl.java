package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.response.AdminStatsResponse;
import com.example.waiter_rating.dto.response.AdminUserResponse;
import com.example.waiter_rating.dto.response.AppUserResponse;
import com.example.waiter_rating.model.*;
import com.example.waiter_rating.model.enums.AuthProvider;
import com.example.waiter_rating.repository.*;
import com.example.waiter_rating.service.AppUserService;
import com.example.waiter_rating.service.EmailService;
import com.example.waiter_rating.service.JwtService;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CvRepo cvRepo;
    private final RatingRepo ratingRepo;
    private final QrTokenRepo qrTokenRepo;
    private final OAuthCodeTokenRepo oAuthCodeTokenRepo;
    private final FavoriteProfessionalRepo favoriteProfessionalRepo;
    private final ProfessionalZoneRepo professionalZoneRepo;
    private final CertificationRepo certificationRepo;
    private final RatingReportRepo ratingReportRepo;
    private final WorkHistoryRepo workHistoryRepo;
    private final EducationRepo educationRepo;

    @Autowired
    public AppUserServiceImpl(AppUserRepo repo,
                              JwtService jwtService,
                              VerificationTokenRepository verificationTokenRepository,
                              PasswordResetTokenRepository passwordResetTokenRepository,
                              EmailService emailService,
                              PasswordEncoder passwordEncoder,
                              CvRepo cvRepo,
                              RatingRepo ratingRepo,
                              QrTokenRepo qrTokenRepo,
                              OAuthCodeTokenRepo oAuthCodeTokenRepo,
                              FavoriteProfessionalRepo favoriteProfessionalRepo,
                              ProfessionalZoneRepo professionalZoneRepo,
                              CertificationRepo certificationRepo,
                              RatingReportRepo ratingReportRepo,
                              WorkHistoryRepo workHistoryRepo,
                              EducationRepo educationRepo) {
        this.repo = repo;
        this.jwtService = jwtService;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.cvRepo = cvRepo;
        this.ratingRepo = ratingRepo;
        this.qrTokenRepo = qrTokenRepo;
        this.oAuthCodeTokenRepo = oAuthCodeTokenRepo;
        this.favoriteProfessionalRepo = favoriteProfessionalRepo;
        this.professionalZoneRepo = professionalZoneRepo;
        this.certificationRepo = certificationRepo;
        this.ratingReportRepo = ratingReportRepo;
        this.workHistoryRepo = workHistoryRepo;
        this.educationRepo = educationRepo;
    }

    @Override
    public AppUserResponse getById(Long id) {
        AppUser user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));
        return mapToResponse(user);
    }

    @Override
    public List<AppUserResponse> listAll() {
        return repo.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> checkUserRoles(String authHeader) {
        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.validateToken(token);
            String email = claims.getSubject();

            AppUser user = repo.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean hasClientRole = true;
            boolean hasProfessionalRole = user.getProfessionType() != null;

            Map<String, Object> response = new HashMap<>();
            response.put("hasClientRole", hasClientRole);
            response.put("hasProfessionalRole", hasProfessionalRole);
            response.put("activeRole", user.getActiveRole().name());
            response.put("canSwitchRole", user.canSwitchRole());

            if (!user.canSwitchRole()) {
                response.put("nextAllowedSwitchDate", user.getNextAllowedRoleSwitchDate());
            }

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error verificando roles: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void createVerificationToken(AppUser user) {
        verificationTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken(token, user);
        verificationTokenRepository.save(verificationToken);
        String role = user.getActiveRole() != null ? user.getActiveRole().name() : "CLIENT";
        emailService.sendVerificationEmail(user.getEmail(), user.getName(), token, role);
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
            return;
        }

        AppUser user = userOpt.get();
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
        emailService.sendWelcomeEmail(user.getEmail(), user.getName(), user.getActiveRole() != null ? user.getActiveRole().name() : "CLIENT");
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
                .map(u -> {
                    int ratingsCount;
                    if (UserRole.CLIENT.equals(u.getActiveRole())) {
                        ratingsCount = u.getRatingsGiven() == null ? 0 : u.getRatingsGiven().size();
                    } else {
                        ratingsCount = u.getTotalRatings() == null ? 0 : u.getTotalRatings();
                    }

                    double avgGiven = u.getRatingsGiven() == null || u.getRatingsGiven().isEmpty()
                            ? 0.0
                            : u.getRatingsGiven().stream()
                            .mapToInt(r -> r.getScore())
                            .average()
                            .orElse(0.0);

                    return new AdminUserResponse(
                            u.getId(),
                            u.getName(),
                            u.getEmail(),
                            u.getActiveRole().name(),
                            u.getSuspended(),
                            u.getEmailVerified(),
                            u.getAuthProvider().name(),
                            u.getCreatedAt(),
                            ratingsCount,
                            avgGiven
                    );
                })
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
    @Transactional
    public void deleteByAdmin(Long id) {
        AppUser user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con id: " + id));

        // Tokens
        verificationTokenRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        oAuthCodeTokenRepo.deleteAll(oAuthCodeTokenRepo.findByUserId(id));

        // Denuncias hechas por el usuario (aplica a cualquier rol)
        ratingReportRepo.deleteAll(ratingReportRepo.findByReporterId(id));

        // Ratings emitidas como cliente
        List<Rating> ratingsEmitidas = ratingRepo.findByClientId(id);
        ratingsEmitidas.forEach(r -> r.setClient(null));
        ratingRepo.saveAll(ratingsEmitidas);

        // Datos de profesional
        certificationRepo.deleteAll(certificationRepo.findByProfessionalId(id));
        educationRepo.deleteByProfessionalId(id);
        workHistoryRepo.deleteAll(workHistoryRepo.findByProfessionalId(id));

        if (user.getCv() != null) {
            Cv cv = cvRepo.findById(user.getCv().getId()).orElse(null);
            if (cv != null) {
                cv.getZones().clear();
                cvRepo.save(cv);
                cvRepo.delete(cv);
            }
        }

        List<Rating> ratingsRecibidos = ratingRepo.findByProfessionalId(id);
        ratingsRecibidos.forEach(r -> r.setProfessional(null));
        ratingRepo.saveAll(ratingsRecibidos);

        qrTokenRepo.deleteAll(qrTokenRepo.findByProfessionalId(id));
        favoriteProfessionalRepo.deleteAll(favoriteProfessionalRepo.findByProfessionalId(id));
        favoriteProfessionalRepo.deleteAll(favoriteProfessionalRepo.findByClientIdOrderBySavedAtDesc(id));

        repo.deleteById(id);
        log.info("Usuario {} eliminado por admin", id);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        List<AppUser> all = repo.findAll();
        long professionals = 0, clients = 0, suspended = 0, totalRatings = 0;
        double reputationSum = 0;
        for (AppUser u : all) {
            if (u.isProfessional()) { professionals++; reputationSum += u.getReputationScore(); }
            else if (u.isClient()) clients++;
            if (u.getSuspended()) suspended++;
            totalRatings += u.getTotalRatings();
        }
        double avgReputation = professionals > 0 ? reputationSum / professionals : 0.0;
        return new AdminStatsResponse((long) all.size(), professionals, clients, suspended, totalRatings, avgReputation);
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