package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.ClientRequest;
import com.example.waiter_rating.dto.response.ClientResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.ProfessionType;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.service.ClientService;
import com.example.waiter_rating.service.CvService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClientServiceImpl implements ClientService {

    private final AppUserRepo appUserRepo;
    private final CvService cvService;

    public ClientServiceImpl(AppUserRepo appUserRepo, CvService cvService) {
        this.appUserRepo = appUserRepo;
        this.cvService = cvService;
    }

    @Override
    @Transactional
    public ClientResponse create(ClientRequest request) {
        // Verificar que no exista el email
        if (appUserRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        AppUser client = AppUser.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(request.getPassword())
                .profilePicture(request.getProfilePicture())
                .provider(request.getProvider() != null ? request.getProvider() : "LOCAL")
                .providerId(request.getProviderId())
                .emailVerified(false)
                .activeRole(UserRole.CLIENT)
                .build();

        client = appUserRepo.save(client);
        return toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientResponse> getById(Long id) {
        return appUserRepo.findById(id)
                .filter(user -> UserRole.CLIENT.equals(user.getActiveRole()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientResponse> getByEmail(String email) {
        return appUserRepo.findByEmail(email)
                .filter(user -> UserRole.CLIENT.equals(user.getActiveRole()))
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> listAll() {
        return appUserRepo.findAll().stream()
                .filter(user -> UserRole.CLIENT.equals(user.getActiveRole()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        AppUser client = appUserRepo.findById(id)
                .filter(user -> UserRole.CLIENT.equals(user.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        client.setName(request.getName());
        client.setProfilePicture(request.getProfilePicture());

        client = appUserRepo.save(client);
        return toResponse(client);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AppUser user = appUserRepo.findById(id)
                .filter(u -> UserRole.CLIENT.equals(u.getActiveRole()))
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        appUserRepo.deleteById(id);
    }

    private ClientResponse toResponse(AppUser client) {
        ClientResponse response = new ClientResponse();
        response.setId(client.getId());
        response.setName(client.getName());
        response.setEmail(client.getEmail());
        response.setProfilePicture(client.getProfilePicture());
        response.setEmailVerified(client.getEmailVerified());
        response.setProvider(client.getProvider());
        response.setCreatedAt(client.getCreatedAt());
        response.setTotalRatingsGiven(
                client.getRatingsGiven() != null ? client.getRatingsGiven().size() : 0
        );
        return response;
    }

    @Override
    @Transactional
    public AppUser findOrCreateFromGoogle(String email, String name, String googleId, Boolean emailVerified) {
        Optional<AppUser> existingUser = appUserRepo.findByEmail(email);

        if (existingUser.isPresent()) {
            AppUser existing = existingUser.get();
            System.out.println("✅ Usuario existente encontrado: " + existing.getName());
            return existing;
        }

        AppUser newClient = AppUser.builder()
                .name(name)
                .email(email)
                .provider("GOOGLE")
                .providerId(googleId)
                .emailVerified(emailVerified != null ? emailVerified : false)
                .activeRole(UserRole.CLIENT)
                .build();

        AppUser saved = appUserRepo.save(newClient);
        System.out.println("✅ Nuevo cliente creado: " + saved.getName() + " (ID: " + saved.getId() + ")");

        return saved;
    }

    @Override
    public AppUser findByEmail(String email) {
        return appUserRepo.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional
    public AppUser upgradeToProfessional(Long clientId, String professionType, String professionalTitle) {
        AppUser client = appUserRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        if (UserRole.PROFESSIONAL.equals(client.getActiveRole())) {
            throw new IllegalStateException("Este usuario ya es un profesional");
        }

        ProfessionType profession;
        try {
            profession = ProfessionType.valueOf(professionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de profesión inválido: " + professionType);
        }

        // Actualizar el mismo usuario a Professional
        client.setActiveRole(UserRole.PROFESSIONAL);
        client.setProfessionType(profession);
        client.setProfessionalTitle(professionalTitle);
        client.setReputationScore(0.0);
        client.setTotalRatings(0);
        client.setSearchable(true);

        AppUser updated = appUserRepo.save(client);
        cvService.getOrCreateForProfessional(updated.getId());

        System.out.println("✅ Cliente " + clientId + " convertido a Professional");

        return updated;
    }
}