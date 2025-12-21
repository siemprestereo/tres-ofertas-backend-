package com.example.waiter_rating.service;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.Client;
import com.example.waiter_rating.model.Professional;

import java.util.Optional;

public interface AuthService {
    Optional<AppUser> getCurrentUser();
    boolean isCurrentUserProfessional();
    boolean isCurrentUserClient();
    Optional<Professional> getCurrentProfessional();
    Optional<Client> getCurrentClient();
}