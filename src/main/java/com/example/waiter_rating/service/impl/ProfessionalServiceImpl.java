package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.ProfessionalRequest;
import com.example.waiter_rating.dto.response.ProfessionalResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.service.ProfessionalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProfessionalServiceImpl implements ProfessionalService {

    private final AppUserRepo appUserRepo;
    private final RatingRepo ratingRepo;

    public ProfessionalServiceImpl(AppUserRepo appUserRepo, RatingRepo ratingRepo) {
        this.appUserRepo = appUserRepo;
        this.ratingRepo = ratingRepo;
    }

    @Override
    @Transactional
    public ProfessionalResponse create(ProfessionalRequest request) {
        // Verificar que no exista el email
        if (appUserRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        AppUser professional = AppUser.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .profilePicture(request.getProfilePicture())
                .provider(request.getProvider() != null ? request.getProvider() : "LOCAL")
                .providerId(request.getProviderId())
                .emailVerified(false)
                .activeRole(UserRole.PROFESSIONAL)
                .professionType(request.getProfessionType())
                .professionalTitle(request.getProfessionType())
                .reputationScore(0.0)
                .totalRatings(0)
                .monthlyWorkplaceChanges(0)
                .searchable(true)
                .build();

        professional = appUserRepo.save(professional);
        return toResponse(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfessionalResponse> getById(Long id) {
        return appUserRepo.findById(id)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfessionalResponse> getByEmail(String email) {
        return appUserRepo.findByEmail(email)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalResponse> listAll() {
        return appUserRepo.findAll().stream()
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalResponse> listByProfessionType(String professionType) {
        return appUserRepo.findAll().stream()
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .filter(p -> professionType.equals(p.getProfessionType()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProfessionalResponse update(Long id, ProfessionalRequest request) {
        AppUser professional = appUserRepo.findById(id)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        professional.setName(request.getName());
        professional.setProfilePicture(request.getProfilePicture());

        if (request.getProfessionType() != null) {
            professional.setProfessionType(request.getProfessionType());
        }

        professional = appUserRepo.save(professional);
        return toResponse(professional);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AppUser user = appUserRepo.findById(id)
                .filter(u -> UserRole.PROFESSIONAL.equals(u.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        appUserRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canChangeWorkplace(Long professionalId) {
        AppUser professional = appUserRepo.findById(professionalId)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        return canChangeWorkplace(professional);
    }

    private boolean canChangeWorkplace(AppUser professional) {
        if (professional.getLastWorkplaceChangeDate() == null) {
            return true;
        }

        YearMonth lastChangeMonth = YearMonth.from(professional.getLastWorkplaceChangeDate());
        YearMonth currentMonth = YearMonth.now();

        if (!lastChangeMonth.equals(currentMonth)) {
            return true;
        }

        return professional.getMonthlyWorkplaceChanges() < 2;
    }

    @Override
    @Transactional
    public void registerWorkplaceChange(Long professionalId) {
        AppUser professional = appUserRepo.findById(professionalId)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        if (!canChangeWorkplace(professional)) {
            throw new IllegalStateException("Has alcanzado el límite de 2 cambios de lugar de trabajo por mes");
        }

        LocalDate now = LocalDate.now();
        LocalDate lastChange = professional.getLastWorkplaceChangeDate();

        if (lastChange == null || YearMonth.from(lastChange).isBefore(YearMonth.from(now))) {
            professional.setMonthlyWorkplaceChanges(1);
        } else {
            professional.setMonthlyWorkplaceChanges(professional.getMonthlyWorkplaceChanges() + 1);
        }

        professional.setLastWorkplaceChangeDate(now);
        appUserRepo.save(professional);
    }

    @Override
    public AppUser findByEmail(String email) {
        return appUserRepo.findByEmail(email)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElse(null);
    }

    @Override
    @Transactional
    public AppUser findOrCreateFromGoogle(String email, String name, String googleId, Boolean emailVerified) {
        Optional<AppUser> existingUser = appUserRepo.findByEmail(email);

        if (existingUser.isPresent()) {
            System.out.println("👤 Usuario existente encontrado: " + email);
            return existingUser.get();
        }

        System.out.println("➕ Creando nuevo profesional desde Google: " + email);
        AppUser newProfessional = AppUser.builder()
                .name(name)
                .email(email)
                .provider("GOOGLE")
                .providerId(googleId)
                .emailVerified(emailVerified != null ? emailVerified : false)
                .activeRole(UserRole.PROFESSIONAL)
                .reputationScore(0.0)
                .totalRatings(0)
                .monthlyWorkplaceChanges(0)
                .searchable(true)
                .build();

        return appUserRepo.save(newProfessional);
    }

    private ProfessionalResponse toResponse(AppUser professional) {
        ProfessionalResponse response = new ProfessionalResponse();
        response.setId(professional.getId());
        response.setName(professional.getName());
        response.setEmail(professional.getEmail());
        response.setPhone(professional.getPhone());
        response.setProfilePicture(professional.getProfilePicture());
        response.setEmailVerified(professional.getEmailVerified());
        response.setProvider(professional.getProvider());
        response.setProfessionType(professional.getProfessionType());
        response.setCreatedAt(professional.getCreatedAt());
        response.setAverageRating(professional.getReputationScore());
        response.setTotalRatings(professional.getTotalRatings());
        response.setMonthlyWorkplaceChanges(professional.getMonthlyWorkplaceChanges());
        response.setCanChangeWorkplace(canChangeWorkplace(professional));
        response.setLocation(professional.getLocation());
        response.setProfessionalTitle(professional.getProfessionalTitle());
        return response;
    }

    @Override
    @Transactional
    public void updateProfessionalReputation(Long professionalId) {
        System.out.println(">>> updateProfessionalReputation called - professionalId: " + professionalId);

        AppUser professional = appUserRepo.findById(professionalId)
                .filter(user -> UserRole.PROFESSIONAL.equals(user.getActiveRole()))
                .orElseThrow(() -> new RuntimeException("Professional not found with id: " + professionalId));

        Double averageScore = ratingRepo.calculateAverageScore(professionalId);
        Long totalRatings = ratingRepo.countRatingsByProfessional(professionalId);

        System.out.println(">>> Calculado - averageScore: " + averageScore + ", totalRatings: " + totalRatings);

        professional.setReputationScore(averageScore != null ? averageScore : 0.0);
        professional.setTotalRatings(totalRatings != null ? totalRatings.intValue() : 0);

        appUserRepo.save(professional);

        System.out.println(">>> Professional guardado - reputationScore: " + professional.getReputationScore() + ", totalRatings: " + professional.getTotalRatings());
    }
}