package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.RatingFromQrRequest;
import com.example.waiter_rating.dto.request.RatingRequest;
import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.ClientRepo;
import com.example.waiter_rating.repository.QrTokenRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.repository.BusinessRepo;
import com.example.waiter_rating.repository.ProfessionalRepo;
import com.example.waiter_rating.repository.WorkHistoryRepo;
import com.example.waiter_rating.service.RatingService;
import com.example.waiter_rating.service.ProfessionalService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RatingServiceImpl implements RatingService {

    // ===== Configurables =====
    private static final int EDIT_WINDOW_MINUTES = 5;
    private static final int RATING_COOLDOWN_MONTHS = 6;

    // ===== Repos =====
    private final RatingRepo ratingRepo;
    private final ClientRepo clientRepo;
    private final ProfessionalRepo professionalRepo;
    private final BusinessRepo businessRepo;
    private final QrTokenRepo qrTokenRepo;
    private final WorkHistoryRepo workHistoryRepo;

    // ===== Services =====
    private final ProfessionalService professionalService;

    public RatingServiceImpl(RatingRepo ratingRepo,
                             ClientRepo clientRepo,
                             ProfessionalRepo professionalRepo,
                             BusinessRepo businessRepo,
                             QrTokenRepo qrTokenRepo,
                             WorkHistoryRepo workHistoryRepo,
                             ProfessionalService professionalService) {
        this.ratingRepo = ratingRepo;
        this.clientRepo = clientRepo;
        this.professionalRepo = professionalRepo;
        this.businessRepo = businessRepo;
        this.qrTokenRepo = qrTokenRepo;
        this.workHistoryRepo = workHistoryRepo;
        this.professionalService = professionalService;
    }

    // =========================================================
    // Alta "directa" (sin QR): ya conocés professionalId y businessId
    // =========================================================
    @Override
    @Transactional
    public Rating submitRating(RatingRequest request) {
        // Validar que tenemos lo mínimo necesario
        if (request.getProfessionalId() == null) {
            throw new IllegalArgumentException("professionalId es obligatorio");
        }

        if (request.getWorkHistoryId() == null) {
            throw new IllegalArgumentException("workHistoryId es obligatorio");
        }

        // Buscar el professional
        Professional professional = professionalRepo.findById(request.getProfessionalId())
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + request.getProfessionalId()));

        // Buscar el WorkHistory específico
        WorkHistory workHistory = workHistoryRepo.findById(request.getWorkHistoryId())
                .orElseThrow(() -> new IllegalArgumentException("WorkHistory no encontrado: " + request.getWorkHistoryId()));

        // Verificar que el WorkHistory pertenece al professional
        if (!workHistory.getProfessional().getId().equals(professional.getId())) {
            throw new IllegalArgumentException("El WorkHistory no pertenece a este Professional");
        }

        // Obtener el Business desde el WorkHistory
        Business business = workHistory.getBusiness();

        // Buscar el cliente (viene del usuario autenticado)
        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepo.findById(request.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.getClientId()));
        }

        // *** VALIDAR COOLDOWN DE 6 MESES ***
        validateRatingCooldown(request.getClientId(), request.getProfessionalId());

        // Crear el rating
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

        System.out.println(">>> ANTES de actualizar reputación - Professional ID: " + savedRating.getProfessional().getId());

        professionalService.updateProfessionalReputation(savedRating.getProfessional().getId());

        System.out.println(">>> DESPUÉS de actualizar reputación");
        return savedRating;
    }

    // =========================================================
    // Alta desde QR: el QR trae el professional; el cliente elige el business
    // =========================================================
    @Override
    @Transactional
    public Rating submitFromQr(String code, @Valid RatingFromQrRequest request) {
        QrToken token = qrTokenRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("QR inválido"));
        if (!token.isValidNow()) {
            throw new IllegalStateException("QR expirado o inactivo");
        }

        Professional professional = token.getProfessional();
        if (professional == null) {
            throw new IllegalStateException("El QR no pertenece a un professional válido");
        }

        if (request.getBusinessId() == null) {
            throw new IllegalArgumentException("Debe indicar businessId al calificar desde QR");
        }
        Business business = businessRepo.findById(request.getBusinessId())
                .orElseThrow(() -> new IllegalArgumentException("Business no encontrado: " + request.getBusinessId()));

        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepo.findById(request.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.getClientId()));
        }

        // *** VALIDAR COOLDOWN DE 6 MESES ***
        validateRatingCooldown(request.getClientId(), professional.getId());

        Rating rating = Rating.builder()
                .professional(professional)
                .business(business)
                .client(client)
                .score(request.getScore())
                .comment(request.getComment())
                .serviceDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Rating saved = ratingRepo.save(rating);

        token.setActive(false);
        qrTokenRepo.save(token);

        professionalService.updateProfessionalReputation(saved.getProfessional().getId());

        return saved;
    }

    /**
     * Valida que el cliente pueda calificar a este profesional
     * (debe haber pasado 6 meses desde la última calificación)
     */
    private void validateRatingCooldown(Long clientId, Long professionalId) {
        if (clientId == null) {
            return; // Si no hay cliente (rating anónimo), no validamos
        }

        Optional<Rating> lastRating = ratingRepo.findLastRatingByClientAndProfessional(clientId, professionalId);

        if (lastRating.isPresent()) {
            LocalDateTime lastRatingDate = lastRating.get().getCreatedAt();
            LocalDateTime nextAllowedDate = lastRatingDate.plusMonths(RATING_COOLDOWN_MONTHS);

            if (LocalDateTime.now().isBefore(nextAllowedDate)) {
                String formattedDate = nextAllowedDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                throw new IllegalStateException(
                        "Ya calificaste a este profesional. Podrás volver a calificarlo a partir del " + formattedDate
                );
            }
        }
    }

    // =========================================================
    // Editar dentro de la ventana de tiempo
    // =========================================================
    @Override
    @Transactional
    public Optional<Rating> updateRating(Long ratingId, Integer newScore, String newComment) {
        return ratingRepo.findById(ratingId).map(r -> {
            ensureEditable(r.getCreatedAt());
            if (newScore != null) r.setScore(newScore);
            if (newComment != null) r.setComment(newComment);
            r.setUpdatedAt(LocalDateTime.now());
            Rating updatedRating = ratingRepo.save(r);

            // *** ACTUALIZAR REPUTACIÓN DEL PROFESSIONAL ***
            professionalService.updateProfessionalReputation(updatedRating.getProfessional().getId());

            return updatedRating;
        });
    }

    // =========================================================
    // Borrar dentro de la ventana de tiempo
    // =========================================================
    @Override
    @Transactional
    public boolean deleteRating(Long ratingId) {
        return ratingRepo.findById(ratingId).map(r -> {
            ensureEditable(r.getCreatedAt());

            // *** GUARDAR EL ID DEL PROFESSIONAL ANTES DE ELIMINAR ***
            Long professionalId = r.getProfessional().getId();

            ratingRepo.delete(r);

            // *** ACTUALIZAR REPUTACIÓN DEL PROFESSIONAL ***
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

    // =========================================================
    // Helpers
    // =========================================================
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
    public List<Rating> getRatingsByProfessionalAndWorkplace(Long professionalId, Long workHistoryId) {
        return ratingRepo.findByProfessionalIdAndWorkHistoryIdOrderByCreatedAtDesc(professionalId, workHistoryId);
    }
}