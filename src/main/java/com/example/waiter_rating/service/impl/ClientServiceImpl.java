package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.ClientRequest;
import com.example.waiter_rating.dto.response.ClientResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.ProfessionType;
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

    private final ClientRepo clientRepo;
    private final AppUserRepo appUserRepo;
    private final ProfessionalRepo professionalRepo;
    private final CvService cvService;

    public ClientServiceImpl(ClientRepo clientRepo, AppUserRepo appUserRepo, ProfessionalRepo professionalRepo, CvService cvService) {
        this.clientRepo = clientRepo;
        this.appUserRepo = appUserRepo;
        this.professionalRepo = professionalRepo;
        this.cvService = cvService;
    }

    @Override
    @Transactional
    public ClientResponse create(ClientRequest request) {
        // Verificar que no exista el email
        if (clientRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }

        Client client = new Client();
        client.setName(request.getName());
        client.setEmail(request.getEmail());
        client.setPassword(request.getPassword());
        client.setProfilePicture(request.getProfilePicture());
        client.setProvider(request.getProvider() != null ? request.getProvider() : "LOCAL");
        client.setProviderId(request.getProviderId());
        client.setEmailVerified(false);
        client.setActiveRole(AppUser.UserRole.CLIENT); // ✅ AGREGADO

        client = clientRepo.save(client);
        return toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientResponse> getById(Long id) {
        return clientRepo.findById(id).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClientResponse> getByEmail(String email) {
        return clientRepo.findByEmail(email).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> listAll() {
        return clientRepo.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = clientRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        client.setName(request.getName());
        client.setProfilePicture(request.getProfilePicture());

        client = clientRepo.save(client);
        return toResponse(client);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!clientRepo.existsById(id)) {
            throw new IllegalArgumentException("Cliente no encontrado");
        }
        clientRepo.deleteById(id);
    }

    private ClientResponse toResponse(Client client) {
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
    public Client findOrCreateFromGoogle(String email, String name, String googleId, Boolean emailVerified) {
        Optional<AppUser> existingUser = appUserRepo.findByEmail(email);

        if (existingUser.isPresent() && existingUser.get() instanceof Client) {
            Client existing = (Client) existingUser.get();
            System.out.println("✅ Cliente existente encontrado: " + existing.getName());
            return existing;
        }

        Client newClient = new Client();
        newClient.setName(name);
        newClient.setEmail(email);
        newClient.setProvider("GOOGLE");
        newClient.setProviderId(googleId);
        newClient.setEmailVerified(emailVerified != null ? emailVerified : false);
        newClient.setActiveRole(AppUser.UserRole.CLIENT); // ✅ AGREGADO

        Client saved = clientRepo.save(newClient);
        System.out.println("✅ Nuevo cliente creado: " + saved.getName() + " (ID: " + saved.getId() + ")");

        return saved;
    }

    @Override
    public Client findByEmail(String email) {
        return clientRepo.findByEmail(email).orElse(null);
    }

    @Override
    @Transactional
    public Professional upgradeToProfessional(Long clientId, String professionType, String professionalTitle) {
        Client client = clientRepo.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        if (!(client instanceof Client)) {
            throw new IllegalStateException("Este usuario ya es un profesional");
        }

        ProfessionType profession;
        try {
            profession = ProfessionType.valueOf(professionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de profesión inválido: " + professionType);
        }

        Professional professional = new Professional();
        professional.setEmail(client.getEmail());
        professional.setName(client.getName());
        professional.setPassword(client.getPassword());
        professional.setProfilePicture(client.getProfilePicture());
        professional.setEmailVerified(client.getEmailVerified());
        professional.setProvider(client.getProvider());
        professional.setProviderId(client.getProviderId());
        professional.setProfessionType(profession);
        professional.setProfessionalTitle(professionalTitle);
        professional.setActiveRole(AppUser.UserRole.PROFESSIONAL); // ✅ AGREGADO

        clientRepo.delete(client);
        professional = professionalRepo.save(professional);
        cvService.getOrCreateForProfessional(professional.getId());

        System.out.println("✅ Cliente " + clientId + " convertido a Professional " + professional.getId());

        return professional;
    }
}