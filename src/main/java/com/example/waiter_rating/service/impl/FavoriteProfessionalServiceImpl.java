package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.response.FavoriteProfessionalResponse;
import com.example.waiter_rating.model.Client;
import com.example.waiter_rating.model.FavoriteProfessional;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.model.Rating;
import com.example.waiter_rating.repository.ClientRepo;
import com.example.waiter_rating.repository.FavoriteProfessionalRepo;
import com.example.waiter_rating.repository.ProfessionalRepo;
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
    private final ClientRepo clientRepo;
    private final ProfessionalRepo professionalRepo;
    private final RatingRepo ratingRepo;

    public FavoriteProfessionalServiceImpl(
            FavoriteProfessionalRepo favoriteProfessionalRepo,
            ClientRepo clientRepo,
            ProfessionalRepo professionalRepo,
            RatingRepo ratingRepo) {
        this.favoriteProfessionalRepo = favoriteProfessionalRepo;
        this.clientRepo = clientRepo;
        this.professionalRepo = professionalRepo;
        this.ratingRepo = ratingRepo;
    }

    @Override
    @Transactional
    public FavoriteProfessionalResponse addFavorite(Long clientId, Long professionalId, String notes) {
        // Verificar que el cliente existe
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        // Verificar que el profesional existe
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Profesional no encontrado"));

        // Verificar si ya está en favoritos
        if (favoriteProfessionalRepo.existsByClientIdAndProfessionalId(clientId, professionalId)) {
            throw new IllegalStateException("Este profesional ya está en tus favoritos");
        }

        // Crear favorito
        FavoriteProfessional favorite = FavoriteProfessional.builder()
                .client(client)
                .professional(professional)
                .notes(notes)
                .build();

        favorite = favoriteProfessionalRepo.save(favorite);

        System.out.println("✅ Profesional " + professionalId + " agregado a favoritos del cliente " + clientId);

        return toResponse(favorite);
    }

    @Override
    @Transactional
    public void removeFavorite(Long clientId, Long professionalId) {
        if (!favoriteProfessionalRepo.existsByClientIdAndProfessionalId(clientId, professionalId)) {
            throw new IllegalArgumentException("Este profesional no está en tus favoritos");
        }

        favoriteProfessionalRepo.deleteByClientIdAndProfessionalId(clientId, professionalId);

        System.out.println("✅ Profesional " + professionalId + " eliminado de favoritos del cliente " + clientId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long clientId, Long professionalId) {
        return favoriteProfessionalRepo.existsByClientIdAndProfessionalId(clientId, professionalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteProfessionalResponse> listFavorites(Long clientId) {
        List<FavoriteProfessional> favorites = favoriteProfessionalRepo
                .findByClientIdOrderBySavedAtDesc(clientId);

        return favorites.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FavoriteProfessionalResponse> listFavoritesWithStats(
            Long clientId,
            LocalDate startDate,
            LocalDate endDate) {

        List<FavoriteProfessional> favorites = favoriteProfessionalRepo
                .findByClientIdOrderBySavedAtDesc(clientId);

        return favorites.stream()
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

    // ========== MAPPERS ==========

    private FavoriteProfessionalResponse toResponse(FavoriteProfessional favorite) {
        Professional prof = favorite.getProfessional();

        return FavoriteProfessionalResponse.builder()
                .favoriteId(favorite.getId())
                .professionalId(prof.getId())
                .professionalName(prof.getName())
                .professionalEmail(prof.getEmail())
                .professionType(prof.getProfessionType().toString())
                .profilePicture(prof.getProfilePicture())
                .reputationScore(prof.getReputationScore())
                .totalRatings(prof.getTotalRatings())
                .savedAt(favorite.getSavedAt())
                .notes(favorite.getNotes())
                .build();
    }

    private FavoriteProfessionalResponse toResponseWithStats(
            FavoriteProfessional favorite,
            LocalDate startDate,
            LocalDate endDate) {

        Professional prof = favorite.getProfessional();

        // Calcular estadísticas en el período
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Rating> ratingsInPeriod = ratingRepo.findByProfessionalIdAndCreatedAtBetween(
                prof.getId(),
                startDateTime,
                endDateTime
        );

        Double avgScore = ratingsInPeriod.stream()
                .mapToDouble(Rating::getScore)
                .average()
                .orElse(0.0);

        return FavoriteProfessionalResponse.builder()
                .favoriteId(favorite.getId())
                .professionalId(prof.getId())
                .professionalName(prof.getName())
                .professionalEmail(prof.getEmail())
                .professionType(prof.getProfessionType().toString())
                .profilePicture(prof.getProfilePicture())
                .reputationScore(avgScore)
                .totalRatings(ratingsInPeriod.size())
                .savedAt(favorite.getSavedAt())
                .notes(favorite.getNotes())
                .build();
    }
}