package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.RatingFromQrRequest;
import com.example.waiter_rating.dto.request.RatingRequest;
import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.QrTokenRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.repository.BusinessRepo;
import com.example.waiter_rating.repository.WorkHistoryRepo;
import com.example.waiter_rating.service.RatingService;
import com.example.waiter_rating.service.ProfessionalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RatingServiceImpl implements RatingService {

    private static final int EDIT_WINDOW_MINUTES = 30;
    private static final int RATING_COOLDOWN_MONTHS = 6;

    private final RatingRepo ratingRepo;
    private final AppUserRepo appUserRepo;
    private final BusinessRepo businessRepo;
    private final QrTokenRepo qrTokenRepo;
    private final WorkHistoryRepo workHistoryRepo;
    private final ProfessionalService professionalService;

    public RatingServiceImpl(RatingRepo ratingRepo,
                             AppUserRepo appUserRepo,
                             BusinessRepo businessRepo,
                             QrTokenRepo qrTokenRepo,
                             WorkHistoryRepo workHistoryRepo,
                             ProfessionalService professionalService) {
        this.ratingRepo = ratingRepo;
        this.appUserRepo = appUserRepo;
        this.businessRepo = businessRepo;
        this.qrTokenRepo = qrTokenRepo;
        this.workHistoryRepo = workHistoryRepo;
        this.professionalService = professionalService;
    }

    @Override
    @Transactional
    public Rating submitRating(RatingRequest request) {
        if (request.getProfessionalId() == null) {
            throw new IllegalArgumentException("professionalId es obligatorio");
        }

        if (request.getWorkHistoryId() == null) {
            throw new IllegalArgumentException("workHistoryId es obligatorio");
        }

        AppUser professional = appUserRepo.findById(request.getProfessionalId())
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + request.getProfessionalId()));

        WorkHistory workHistory = workHistoryRepo.findById(request.getWorkHistoryId())
                .orElseThrow(() -> new IllegalArgumentException("WorkHistory no encontrado: " + request.getWorkHistoryId()));

        if (!workHistory.getProfessional().getId().equals(professional.getId())) {
            throw new IllegalArgumentException("El WorkHistory no pertenece a este Professional");
        }

        Business business = workHistory.getBusiness();

        AppUser client = null;
        if (request.getClientId() != null) {
            client = appUserRepo.findById(request.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.getClientId()));
        }

        validateRatingCooldown(request.getClientId(), request.getProfessionalId());

        Rating rating = Rating.builder()
                .professional(professional)
                .business(business)
                .workHistory(workHistory)
                .client(client)
                .score(request.getScore())
                .comment(request.getComment())
                .serviceDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Rating savedRating = ratingRepo.save(rating);
        professionalService.updateProfessionalReputation(savedRating.getProfessional().getId());
        return savedRating;
    }

    @Override
    @Transactional
    public Rating submitFromQr(String code, @Valid RatingFromQrRequest request) {
        QrToken token = qrTokenRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("QR inválido"));

        if (!token.isValidNow()) {
            throw new IllegalStateException("QR expirado o inactivo");
        }

        AppUser professional = token.getProfessional();
        if (professional == null || !UserRole.PROFESSIONAL.equals(professional.getActiveRole())) {
            throw new IllegalStateException("El QR no pertenece a un professional válido");
        }

        // Validación del lugar de trabajo elegido por el cliente
        WorkHistory workHistory = workHistoryRepo.findById(request.getWorkHistoryId())
                .orElseThrow(() -> new IllegalArgumentException("WorkHistory no encontrado"));

        if (!workHistory.getProfessional().getId().equals(professional.getId())) {
            throw new IllegalArgumentException("El lugar de trabajo seleccionado no corresponde a este profesional");
        }

        Business business = workHistory.getBusiness();

        AppUser client = null;
        if (request.getClientId() != null) {
            client = appUserRepo.findById(request.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.getClientId()));
        }

        validateRatingCooldown(request.getClientId(), professional.getId());

        Rating rating = Rating.builder()
                .professional(professional)
                .business(business)
                .workHistory(workHistory)
                .client(client)
                .score(request.getScore())
                .comment(request.getComment())
                .serviceDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Rating saved = ratingRepo.save(rating);

        // Se mantiene activo el token para permitir múltiples escaneos en la mesa (multiusuario)
        professionalService.updateProfessionalReputation(saved.getProfessional().getId());

        return saved;
    }

    private void validateRatingCooldown(Long clientId, Long professionalId) {
        if (clientId == null) return;

        Optional<Rating> lastRating = ratingRepo.findLastRatingByClientAndProfessional(clientId, professionalId);

        if (lastRating.isPresent()) {
            LocalDateTime lastRatingDate = lastRating.get().getCreatedAt();
            LocalDateTime nextAllowedDate = lastRatingDate.plusMonths(RATING_COOLDOWN_MONTHS);

            if (LocalDateTime.now().isBefore(nextAllowedDate)) {
                String formattedDate = nextAllowedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                throw new IllegalStateException(
                        "Aún no han transcurrido 6 meses desde tu última calificación. Podrás volver a calificar a partir del " + formattedDate
                );
            }
        }
    }

    @Override
    @Transactional
    public Optional<Rating> updateRating(Long ratingId, Integer newScore, String newComment) {
        return ratingRepo.findById(ratingId).map(r -> {
            ensureEditable(r.getCreatedAt());
            if (newScore != null) r.setScore(newScore);
            if (newComment != null) r.setComment(newComment);
            r.setUpdatedAt(LocalDateTime.now());
            Rating updatedRating = ratingRepo.save(r);
            professionalService.updateProfessionalReputation(updatedRating.getProfessional().getId());
            return updatedRating;
        });
    }

    @Override
    @Transactional
    public boolean deleteRating(Long ratingId) {
        return ratingRepo.findById(ratingId).map(r -> {
            ensureEditable(r.getCreatedAt());
            Long professionalId = r.getProfessional().getId();
            ratingRepo.delete(r);
            professionalService.updateProfessionalReputation(professionalId);
            return true;
        }).orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rating> getRatingsForProfessional(Long professionalId) {
        return ratingRepo.findByProfessionalIdOrderByCreatedAtDesc(professionalId);
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageScoreForProfessional(Long professionalId) {
        return ratingRepo.findAverageScoreByProfessionalId(professionalId).orElse(0.0);
    }

    private void ensureEditable(LocalDateTime createdAt) {
        if (createdAt == null) return;
        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        if (minutes > EDIT_WINDOW_MINUTES) {
            throw new IllegalStateException(
                    "La calificación solo puede editarse o eliminarse dentro de los " + EDIT_WINDOW_MINUTES + " minutos posteriores a su creación."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rating> getRatingById(Long id) {
        return ratingRepo.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rating> getRatingsByClient(Long clientId) {
        return ratingRepo.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rating> getRatingsByClient(Long clientId, Integer limit) {
        if (limit != null && limit > 0) {
            Pageable pageable = PageRequest.of(0, limit);
            return ratingRepo.findByClientIdOrderByCreatedAtDesc(clientId, pageable);
        }
        return ratingRepo.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Rating> getRatingsByProfessionalAndWorkplace(Long professionalId, Long workHistoryId) {
        return ratingRepo.findByProfessionalIdAndWorkHistoryIdOrderByCreatedAtDesc(professionalId, workHistoryId);
    }

    @Override
    public List<Rating> getRatingsByWorkHistory(Long workHistoryId) {
        return ratingRepo.findByWorkHistoryId(workHistoryId);
    }
}