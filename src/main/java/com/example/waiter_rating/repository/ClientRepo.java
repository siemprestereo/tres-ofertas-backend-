package com.example.waiter_rating.repository;

import com.example.waiter_rating.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepo extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
}