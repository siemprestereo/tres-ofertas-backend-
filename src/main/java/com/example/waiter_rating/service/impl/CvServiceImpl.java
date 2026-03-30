package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.Cv;
import com.example.waiter_rating.model.Rating;
import com.example.waiter_rating.repository.AppUserRepo;
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
    private final AppUserRepo appUserRepo;
    private final RatingRepo ratingRepo;

    public CvServiceImpl(CvRepo cvRepo, AppUserRepo appUserRepo, RatingRepo ratingRepo) {
        this.cvRepo = cvRepo;
        this.appUserRepo = appUserRepo;
        this.ratingRepo = ratingRepo;
    }

    @Override
    @Transactional
    public Cv getOrCreateForProfessional(Long professionalId) {
        AppUser professional = appUserRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + professionalId));

        Optional<Cv> existing = cvRepo.findByProfessionalId(professionalId);

        if (existing.isPresent()) {
            return existing.get();
        }

        Cv cv = Cv.builder()
                .professional(professional)
                .description("")
                .reputationScore(professional.getReputationScore() != null ? professional.getReputationScore() : 0.0)
                .totalRatings(professional.getTotalRatings() != null ? professional.getTotalRatings() : 0)
                .build();

        return cvRepo.save(cv);
    }

    @Override
    @Transactional(readOnly = true)
    public Cv getPublicCv(Long professionalId) {
        Cv cv = cvRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("CV no encontrado para el professional: " + professionalId));

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
    public Cv updateSkills(Long professionalId, String skills) {
        Cv cv = cvRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("CV no encontrado para el professional: " + professionalId));

        cv.setSkills(skills);
        return cvRepo.save(cv);
    }

    @Override
    @Transactional
    public Cv updateDescriptionAndSkills(Long professionalId, String description, String skills) {
        Cv cv = cvRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("CV no encontrado para el professional: " + professionalId));
        cv.setDescription(description);
        cv.setSkills(skills);
        return cvRepo.save(cv);
    }

    @Override
    @Transactional
    public Cv updateReputationScore(Long professionalId) {
        Cv cv = cvRepo.findByProfessionalId(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("CV no encontrado para el professional: " + professionalId));

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