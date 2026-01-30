package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.ProfessionalRequest;
import com.example.waiter_rating.dto.response.ProfessionalResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.ProfessionType;
import com.example.waiter_rating.repository.RatingRepo;
import com.example.waiter_rating.service.ProfessionalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProfessionalServiceImpl implements ProfessionalService {

    private final ProfessionalRepo professionalRepo;
    private final RatingRepo ratingRepo;

    public ProfessionalServiceImpl(ProfessionalRepo professionalRepo, RatingRepo ratingRepo) {
        this.professionalRepo = professionalRepo;
        this.ratingRepo = ratingRepo;
    }

    @Override
    @Transactional
    public ProfessionalResponse create(ProfessionalRequest request) {
        // Verificar que no exista el email
        if (professionalRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        Professional professional = new Professional();
        professional.setName(request.getName());
        professional.setEmail(request.getEmail());
        professional.setPassword(request.getPassword());
        professional.setProfilePicture(request.getProfilePicture());
        professional.setProvider(request.getProvider() != null ? request.getProvider() : "LOCAL");
        professional.setProviderId(request.getProviderId());
        professional.setEmailVerified(false);
        professional.setProfessionType(request.getProfessionType());
        professional.setMonthlyWorkplaceChanges(0);
        professional.setActiveRole(AppUser.UserRole.PROFESSIONAL); // ✅ AGREGADO

        professional = professionalRepo.save(professional);
        return toResponse(professional);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfessionalResponse> getById(Long id) {
        return professionalRepo.findById(id).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProfessionalResponse> getByEmail(String email) {
        return professionalRepo.findByEmail(email).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalResponse> listAll() {
        return professionalRepo.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfessionalResponse> listByProfessionType(ProfessionType professionType) {
        return professionalRepo.findAll().stream()
                .filter(p -> p.getProfessionType() == professionType)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProfessionalResponse update(Long id, ProfessionalRequest request) {
        Professional professional = professionalRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        professional.setName(request.getName());
        professional.setProfilePicture(request.getProfilePicture());
        // Permitir cambiar profession type si es necesario
        if (request.getProfessionType() != null) {
            professional.setProfessionType(request.getProfessionType());
        }
        // No permitir cambiar email ni password aquí

        professional = professionalRepo.save(professional);
        return toResponse(professional);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!professionalRepo.existsById(id)) {
            throw new IllegalArgumentException("Professional no encontrado");
        }
        professionalRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canChangeWorkplace(Long professionalId) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));
        return professional.canChangeWorkplace();
    }

    @Override
    @Transactional
    public void registerWorkplaceChange(Long professionalId) {
        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new IllegalArgumentException("Professional no encontrado"));

        if (!professional.canChangeWorkplace()) {
            throw new IllegalStateException("Has alcanzado el límite de 2 cambios de lugar de trabajo por mes");
        }

        professional.registerWorkplaceChange();
        professionalRepo.save(professional);
    }

    @Override
    public Professional findByEmail(String email) {
        return professionalRepo.findByEmail(email).orElse(null);
    }

    // ========== NUEVO MÉTODO PARA OAUTH ==========
    @Override
    @Transactional
    public Professional findOrCreateFromGoogle(String email, String name, String googleId, Boolean emailVerified) {
        Optional<Professional> existingProfessional = professionalRepo.findByEmail(email);

        if (existingProfessional.isPresent()) {
            System.out.println("👤 Profesional existente encontrado: " + email);
            return existingProfessional.get();
        }

        System.out.println("➕ Creando nuevo profesional desde Google: " + email);
        Professional newProfessional = new Professional();
        newProfessional.setName(name);
        newProfessional.setEmail(email);
        newProfessional.setProvider("GOOGLE");
        newProfessional.setProviderId(googleId);
        newProfessional.setEmailVerified(emailVerified != null ? emailVerified : false);
        newProfessional.setMonthlyWorkplaceChanges(0);
        newProfessional.setActiveRole(AppUser.UserRole.PROFESSIONAL); // ✅ AGREGADO

        return professionalRepo.save(newProfessional);
    }

    // ========== Mapper ==========
    private ProfessionalResponse toResponse(Professional professional) {
        ProfessionalResponse response = new ProfessionalResponse();
        response.setId(professional.getId());
        response.setName(professional.getName());
        response.setEmail(professional.getEmail());
        response.setProfilePicture(professional.getProfilePicture());
        response.setEmailVerified(professional.getEmailVerified());
        response.setProvider(professional.getProvider());
        response.setProfessionType(professional.getProfessionType()); // NUEVO
        response.setCreatedAt(professional.getCreatedAt());
        response.setAverageRating(professional.getAverageRating());
        response.setTotalRatings(professional.getTotalRatings());
        response.setMonthlyWorkplaceChanges(professional.getMonthlyWorkplaceChanges());
        response.setCanChangeWorkplace(professional.canChangeWorkplace());
        return response;
    }

    @Override
    @Transactional
    public void updateProfessionalReputation(Long professionalId) {

        System.out.println(">>> updateProfessionalReputation called - professionalId: " + professionalId);


        Professional professional = professionalRepo.findById(professionalId)
                .orElseThrow(() -> new RuntimeException("Professional not found with id: " + professionalId));

        // Calcular promedio (puede ser null si no hay ratings)
        Double averageScore = ratingRepo.calculateAverageScore(professionalId);

        // Contar total de ratings
        Long totalRatings = ratingRepo.countRatingsByProfessional(professionalId);

        System.out.println(">>> Calculado - averageScore: " + averageScore + ", totalRatings: " + totalRatings);

        // Actualizar campos
        professional.setReputationScore(averageScore != null ? averageScore : 0.0);
        professional.setTotalRatings(totalRatings != null ? totalRatings.intValue() : 0);

        // Guardar cambios
        professionalRepo.save(professional);

        System.out.println(">>> Professional guardado - reputationScore: " + professional.getReputationScore() + ", totalRatings: " + professional.getTotalRatings());
    }
}