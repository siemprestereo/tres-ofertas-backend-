package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.dto.request.AppUserRequest;
import com.example.waiter_rating.dto.response.AppUserResponse;
import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.Client;
import com.example.waiter_rating.model.Professional;
import com.example.waiter_rating.model.ProfessionType;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.service.AppUserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppUserServiceImpl implements AppUserService {

    private final AppUserRepo repo;

    public AppUserServiceImpl(AppUserRepo repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public AppUserResponse create(AppUserRequest request) {
        AppUser user;

        // Decidir si crear Client o Professional según el userType del request
        if ("PROFESSIONAL".equalsIgnoreCase(request.getUserType())) {
            Professional professional = new Professional();
            professional.setName(request.getName());
            professional.setEmail(request.getEmail());
            professional.setProfilePicture(request.getProfilePicture());
            professional.setEmailVerified(false);
            professional.setMonthlyWorkplaceChanges(0);

            // Establecer profession type (por defecto WAITER si no se especifica)
            professional.setProfessionType(
                    request.getProfessionType() != null
                            ? request.getProfessionType()
                            : ProfessionType.WAITER
            );

            user = professional;
        } else {
            // Por defecto crear Client
            Client client = new Client();
            client.setName(request.getName());
            client.setEmail(request.getEmail());
            client.setProfilePicture(request.getProfilePicture());
            client.setEmailVerified(false);
            user = client;
        }

        user = repo.save(user);
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AppUserResponse getById(Long id) {
        AppUser u = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
        return toResponse(u);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUserResponse> listAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    // ---------- Mapping helper ----------
    private AppUserResponse toResponse(AppUser u) {
        AppUserResponse r = new AppUserResponse();
        r.setId(u.getId());
        r.setName(u.getName());
        r.setEmail(u.getEmail());
        r.setUserType(u.getUserType()); // Usa el método abstracto
        r.setProfilePicture(u.getProfilePicture());

        // Si es Professional, incluir professionType
        if (u instanceof Professional) {
            r.setProfessionType(((Professional) u).getProfessionType());
        }

        return r;
    }
}