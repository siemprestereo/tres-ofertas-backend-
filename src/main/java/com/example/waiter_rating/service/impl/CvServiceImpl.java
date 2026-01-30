package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.Cv;
import com.example.waiter_rating.model.Rating;
import com.example.waiter_rating.repository.CvRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.service.CvService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CvServiceImpl implements CvService {

    private final CvRepo cvRepo;
    private final ProfessionalRepo professionalRepo;
    private final RatingRepo ratingRepo;

    public CvServiceImpl(CvRepo cvRepo, ProfessionalRepo professionalRepo, RatingRepo ratingRepo) {
        this.cvRepo = cvRepo;
        this.professionalRepo = professionalRepo;
        this.ratingRepo = ratingRepo;
    }

    @Override
    @Transactional
    public Cv getOrCreateForProfessional(Long professionalId) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado: " + professionalId));

        Optional<Cv> existing = cvRepo.findByProfessionalId(professionalId);

        if (existing.isPresent()) {
            return existing.get();
        }

        // Crear nuevo CV
        Cv cv = Cv.builder()
                .professional(professional)
                .description("")
                .reputationScore(0.0)
                .totalRatings(0)
                .build();

        return cvRepo.save(cv);
    }

    @Override
    @Transactional(readOnly = true)
    public Cv getPublicCv(Long professionalId) {
        Cv cv = cvRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("CV no encontrado para el professional: " + professionalId));

        // Calcular reputación en tiempo real desde el Professional
        cv.updateReputationFromProfessional();

        return cv;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Cv> getByPublicSlug(String slug) {
        return cvRepo.findByPublicSlug(slug);
    }

    @Override
    @Transactional
    public Cv updateDescription(Long professionalId, String description) {
        Cv cv = cvRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("CV no encontrado para el professional: " + professionalId));

        cv.setDescription(description);
        return cvRepo.save(cv);
    }

    @Override
    @Transactional
    public Cv updateReputationScore(Long professionalId) {
        Cv cv = cvRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("CV no encontrado para el professional: " + professionalId));

        // Actualizar reputación desde el Professional
        recalculateReputationInternal(cv);

        return cvRepo.save(cv);
    }

    @Override
    @Transactional
    public void delete(Long cvId) {
        if (!cvRepo.existsById(cvId)) {
            throw new IllegalArgumentException("CV no encontrado: " + cvId);
        }
        cvRepo.deleteById(cvId);
    }

    /**
     * Recalcula la reputación basándose en los ratings del professional
     */
    private void recalculateReputationInternal(Cv cv) {
        List<Rating> ratings = ratingRepo.findByProfessionalId(cv.getProfessional().getId());

        if (ratings.isEmpty()) {
            cv.setReputationScore(0.0);
            cv.setTotalRatings(0);
        } else {
            double avg = ratings.stream()
                    .mapToInt(Rating::getScore)
                    .average()
                    .orElse(0.0);
            cv.setReputationScore(avg);
            cv.setTotalRatings(ratings.size());
        }
    }

    @Override
    public Cv getCvById(Long cvId) {
        return cvRepo.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV no encontrado con ID: " + cvId));
    }
}