package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.Business;
import com.example.waiter_rating.repository.BusinessRepo;
import com.example.waiter_rating.service.BusinessService;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusinessServiceImpl implements BusinessService {

    private final BusinessRepo repo;

    public BusinessServiceImpl(BusinessRepo repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public Business create(Business business) {
        return repo.save(business);
    }

    @Override
    @Transactional(readOnly = true)
    public Business getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurante no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Business> listAll() {
        return repo.findAll();
    }
}

