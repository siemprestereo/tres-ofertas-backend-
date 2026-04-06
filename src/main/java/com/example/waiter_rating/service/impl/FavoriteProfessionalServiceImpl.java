package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.response.FavoriteProfessionalResponse;
import com.example.waiter_rating.dto.response.WorkHistoryResponse;
import com.example.waiter_rating.dto.response.ZoneResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.FavoriteProfessional;
import com.example.waiter_rating.model.Rating;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.model.WorkHistory;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.CvRepo;
import com.example.waiter_rating.repository.FavoriteProfessionalRepo;
import com.example.waiter_rating.repository.ProfessionalZoneRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.service.FavoriteProfessionalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteProfessionalServiceImpl implements FavoriteProfessionalService {

    private final FavoriteProfessionalRepo favoriteProfessionalRepo;
    private final AppUserRepo appUserRepo;
    private final RatingRepo ratingRepo;
    private final ProfessionalZoneRepo professionalZoneRepo;
    private final CvRepo cvRepo;

    public FavoriteProfessionalServiceImpl(
            FavoriteProfessionalRepo favoriteProfessionalRepo,
            AppUserRepo appUserRepo,
            RatingRepo ratingRepo,
            ProfessionalZoneRepo professionalZoneRepo,
            CvRepo cvRepo) {
        this.favoriteProfessionalRepo = favoriteProfessionalRepo;
        this.appUserRepo = appUserRepo;
        this.ratingRepo = ratingRepo;
        this.professionalZoneRepo = professionalZoneRepo;
        this.cvRepo = cvRepo;
    }

    @Override
    @Transactional
    public FavoriteProfessionalResponse addFavorite(Long clientId, Long professionalId, String notes) {
        AppUser client = appUserRepo.findById(clientId)
                .filter(user -> UserRole.CLIENT.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        AppUser professional = appUserRepo.findById(professionalId)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado"));

        if (favoriteProfessionalRepo.existsByClientIdAndProfessionalId(clientId, professionalId)) {
            throw new IllegalStateException("Este profesional ya está en tus favoritos");
        }

        FavoriteProfessional favorite = FavoriteProfessional.builder()
                .client(client)
                .professional(professional)
                .notes(notes)
                .build();

        favorite = favoriteProfessionalRepo.save(favorite);
        return toResponse(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long clientId, Long professionalId) {
        if (!favoriteProfessionalRepo.existsByClientIdAndProfessionalId(clientId, professionalId)) {
            throw new IllegalArgumentException("Este profesional no está en tus favoritos");
        }
        favoriteProfessionalRepo.deleteByClientIdAndProfessionalId(clientId, professionalId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long clientId, Long professionalId) {
        return favoriteProfessionalRepo.existsByClientIdAndProfessionalId(clientId, professionalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteProfessionalResponse> listFavorites(Long clientId) {
        return favoriteProfessionalRepo
                .findByClientIdOrderBySavedAtDesc(clientId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteProfessionalResponse> listFavoritesWithStats(
            Long clientId, LocalDate startDate, LocalDate endDate) {
        return favoriteProfessionalRepo
                .findByClientIdOrderBySavedAtDesc(clientId)
                .stream()
                .map(fav -> toResponseWithStats(fav, startDate, endDate))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FavoriteProfessionalResponse updateNotes(Long clientId, Long professionalId, String notes) {
        FavoriteProfessional favorite = favoriteProfessionalRepo
                .findByClientIdAndProfessionalId(clientId, professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Favorito no encontrado"));
        favorite.setNotes(notes);
        favorite = favoriteProfessionalRepo.save(favorite);
        return toResponse(favorite);
    }

    @Override
    @Transactional(readOnly = true)
    public long countFavorites(Long clientId) {
        return favoriteProfessionalRepo.countByClientId(clientId);
    }

    // ========== HELPERS ==========

    private List<ZoneResponse> getZones(AppUser prof) {
        if (prof.getCv() == null) return List.of();
        return professionalZoneRepo.findByCvId(prof.getCv().getId()).stream()
                .map(z -> {
                    ZoneResponse zr = new ZoneResponse();
                    zr.setId(z.getId());
                    zr.setProvincia(z.getProvincia());
                    zr.setZona(z.getZona());
                    return zr;
                })
                .collect(Collectors.toList());
    }

    private FavoriteProfessionalResponse toResponse(FavoriteProfessional favorite) {
        AppUser prof = favorite.getProfessional();

        List<WorkHistoryResponse> workHistoryList = prof.getWorkHistory().stream()
                .map(this::mapWorkHistoryToResponse)
                .collect(Collectors.toList());

        String publicSlug = cvRepo.findByProfessionalId(prof.getId())
                .map(cv -> cv.getPublicSlug()).orElse(null);
        java.util.List<String> professionTypes = prof.getProfessionTypes() != null
                ? new java.util.ArrayList<>(prof.getProfessionTypes()) : java.util.List.of();

        return FavoriteProfessionalResponse.builder()
                .favoriteId(favorite.getId())
                .professionalId(prof.getId())
                .publicSlug(publicSlug)
                .professionTypes(professionTypes)
                .professionalName(prof.getName())
                .professionalEmail(prof.getEmail())
                .professionType(prof.getProfessionType())
                .profilePicture(prof.getProfilePicture())
                .reputationScore(prof.getReputationScore())
                .totalRatings(prof.getTotalRatings())
                .savedAt(favorite.getSavedAt())
                .notes(favorite.getNotes())
                .workHistory(workHistoryList)
                .zones(getZones(prof))
                .build();
    }

    private FavoriteProfessionalResponse toResponseWithStats(
            FavoriteProfessional favorite, LocalDate startDate, LocalDate endDate) {

        AppUser prof = favorite.getProfessional();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Rating> allRatingsInPeriod = ratingRepo.findByProfessionalIdAndCreatedAtBetween(
                prof.getId(), startDateTime, endDateTime);

        Double generalAvgScore = allRatingsInPeriod.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);

        List<WorkHistoryResponse> workHistoryList = prof.getWorkHistory().stream()
                .map(work -> mapWorkHistoryWithStats(work, startDateTime, endDateTime))
                .collect(Collectors.toList());

        String publicSlug = cvRepo.findByProfessionalId(prof.getId())
                .map(cv -> cv.getPublicSlug()).orElse(null);
        java.util.List<String> professionTypes2 = prof.getProfessionTypes() != null
                ? new java.util.ArrayList<>(prof.getProfessionTypes()) : java.util.List.of();

        return FavoriteProfessionalResponse.builder()
                .favoriteId(favorite.getId())
                .professionalId(prof.getId())
                .publicSlug(publicSlug)
                .professionTypes(professionTypes2)
                .professionalName(prof.getName())
                .professionalEmail(prof.getEmail())
                .professionType(prof.getProfessionType())
                .profilePicture(prof.getProfilePicture())
                .reputationScore(Math.round(generalAvgScore * 10.0) / 10.0)
                .totalRatings(allRatingsInPeriod.size())
                .savedAt(favorite.getSavedAt())
                .notes(favorite.getNotes())
                .workHistory(workHistoryList)
                .zones(getZones(prof))
                .build();
    }

    private WorkHistoryResponse mapWorkHistoryToResponse(WorkHistory work) {
        WorkHistoryResponse response = new WorkHistoryResponse();
        response.setId(work.getId());
        response.setBusinessId(work.getBusiness() != null ? work.getBusiness().getId() : null);
        response.setBusinessName(work.getBusiness() != null ? work.getBusiness().getName() : work.getBusinessName());
        response.setBusinessType(work.getBusiness() != null ? work.getBusiness().getBusinessType() : null);
        response.setPosition(work.getPosition());
        response.setStartDate(work.getStartDate() != null ? work.getStartDate().toString() : null);
        response.setEndDate(work.getEndDate() != null ? work.getEndDate().toString() : null);
        response.setIsActive(work.getIsActive());
        response.setIsFreelance(work.getIsFreelance());
        response.setReferenceContact(work.getReferenceContact());

        List<Rating> allWorkRatings = ratingRepo.findByWorkHistoryId(work.getId());
        Double avgScore = allWorkRatings.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
        response.setRatingsCountInPeriod(allWorkRatings.size());
        response.setAvgScoreInPeriod(Math.round(avgScore * 10.0) / 10.0);
        return response;
    }

    private WorkHistoryResponse mapWorkHistoryWithStats(
            WorkHistory work, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        WorkHistoryResponse response = new WorkHistoryResponse();
        response.setId(work.getId());
        response.setBusinessId(work.getBusiness() != null ? work.getBusiness().getId() : null);
        response.setBusinessName(work.getBusiness() != null ? work.getBusiness().getName() : work.getBusinessName());
        response.setBusinessType(work.getBusiness() != null ? work.getBusiness().getBusinessType() : null);
        response.setPosition(work.getPosition());
        response.setStartDate(work.getStartDate() != null ? work.getStartDate().toString() : null);
        response.setEndDate(work.getEndDate() != null ? work.getEndDate().toString() : null);
        response.setIsActive(work.getIsActive());
        response.setIsFreelance(work.getIsFreelance());
        response.setReferenceContact(work.getReferenceContact());

        List<Rating> workRatingsInPeriod = ratingRepo.findByWorkHistoryIdAndCreatedAtBetween(
                work.getId(), startDateTime, endDateTime);
        Double avgScore = workRatingsInPeriod.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);
        response.setRatingsCountInPeriod(workRatingsInPeriod.size());
        response.setAvgScoreInPeriod(Math.round(avgScore * 10.0) / 10.0);
        return response;
    }
}