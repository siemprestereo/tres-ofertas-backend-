package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.RatingFromQrRequest;
import com.example.waiter_rating.dto.request.RatingRequest;
import com.example.waiter_rating.model.*;
import com.example.waiter_rating.repository.ClientRepo;
import com.example.waiter_rating.repository.QrTokenRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.repository.BusinessRepo;
import com.example.waiter_rating.repository.ProfessionalRepo;
import com.example.waiter_rating.service.RatingService;
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

    // ===== Repos =====
    private final RatingRepo ratingRepo;
    private final ClientRepo clientRepo;
    private final ProfessionalRepo professionalRepo;
    private final BusinessRepo businessRepo;
    private final QrTokenRepo qrTokenRepo;

    public RatingServiceImpl(RatingRepo ratingRepo,
                             ClientRepo clientRepo,
                             ProfessionalRepo professionalRepo,
                             BusinessRepo businessRepo,
                             QrTokenRepo qrTokenRepo) {
        this.ratingRepo = ratingRepo;
        this.clientRepo = clientRepo;
        this.professionalRepo = professionalRepo;
        this.businessRepo = businessRepo;
        this.qrTokenRepo = qrTokenRepo;
    }

    // =========================================================
    // Alta "directa" (sin QR): ya conocés professionalId y businessId
    // =========================================================
    @Override
    @Transactional
    public Rating submitRating(RatingRequest request) {
        if (request.getProfessionalId() == null || request.getBusinessId() == null) {
            throw new IllegalArgumentException("professionalId y businessId son obligatorios para submitRating");
        }

        Professional professional = professionalRepo.findById(request.getProfessionalId())
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + request.getProfessionalId()));

        Business business = businessRepo.findById(request.getBusinessId())
                .orElseThrow(() -> new IllegalArgumentException("Business no encontrado: " + request.getBusinessId()));

        Client client = null;
        if (request.getClientId() != null) {
            client = clientRepo.findById(request.getClientId())
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado: " + request.getClientId()));
        }

        Rating rating = Rating.builder()
                .professional(professional)
                .business(business)
                .client(client)
                .score(request.getScore())
                .comment(request.getComment())
                .serviceDate(LocalDateTime.now()) // Fecha del servicio
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return ratingRepo.save(rating);
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

        Rating rating = Rating.builder()
                .professional(professional)
                .business(business)
                .client(client)
                .score(request.getScore())
                .comment(request.getComment())
                .serviceDate(LocalDateTime.now()) // Fecha del servicio
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Rating saved = ratingRepo.save(rating);

        // Política actual: un QR se invalida tras usarse
        token.setActive(false);
        qrTokenRepo.save(token);

        return saved;
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
            return ratingRepo.save(r);
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
            ratingRepo.delete(r);
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