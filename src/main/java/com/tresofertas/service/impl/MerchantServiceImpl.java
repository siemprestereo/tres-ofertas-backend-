package com.tresofertas.service.impl;

import com.tresofertas.dto.request.CreateMerchantRequest;
import com.tresofertas.dto.response.MerchantResponse;
import com.tresofertas.model.AppUser;
import com.tresofertas.model.Merchant;
import com.tresofertas.repository.AppUserRepo;
import com.tresofertas.repository.MerchantRepo;
import com.tresofertas.service.MerchantService;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MerchantServiceImpl implements MerchantService {

    private final MerchantRepo merchantRepo;
    private final AppUserRepo userRepo;

    public MerchantServiceImpl(MerchantRepo merchantRepo, AppUserRepo userRepo) {
        this.merchantRepo = merchantRepo;
        this.userRepo = userRepo;
    }

    @Override
    public MerchantResponse create(CreateMerchantRequest request, Long userId) {
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() != AppUser.Role.MERCHANT) {
            throw new RuntimeException("Solo los comerciantes pueden crear un comercio");
        }

        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre es requerido");
        }
        if (request.getCategory() == null || request.getCategory().isBlank()) {
            throw new IllegalArgumentException("La categoría es requerida");
        }
        if (request.getAddress() == null || request.getAddress().isBlank()) {
            throw new IllegalArgumentException("La dirección es requerida");
        }

        String slug = generateSlug(request.getName());

        Merchant merchant = new Merchant();
        merchant.setUser(user);
        merchant.setName(request.getName());
        merchant.setSlug(slug);
        merchant.setCategory(request.getCategory());
        merchant.setSubCategory(request.getSubCategory());
        merchant.setAddress(request.getAddress());
        merchant.setLat(request.getLat());
        merchant.setLng(request.getLng());
        merchant.setWhatsapp(request.getWhatsapp());
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setSchedule(request.getSchedule());

        return MerchantResponse.from(merchantRepo.save(merchant));
    }

    @Override
    public MerchantResponse getBySlug(String slug) {
        Merchant merchant = merchantRepo.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Comercio no encontrado"));
        return MerchantResponse.from(merchant);
    }

    @Override
    public List<MerchantResponse> getMyMerchants(Long userId) {
        return merchantRepo.findByUserId(userId).stream()
                .map(MerchantResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public MerchantResponse update(Long merchantId, CreateMerchantRequest request, Long userId) {
        Merchant merchant = merchantRepo.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Comercio no encontrado"));

        if (!merchant.getUser().getId().equals(userId)) {
            throw new RuntimeException("No tenés permiso para editar este comercio");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            merchant.setName(request.getName());
        }
        if (request.getCategory() != null) merchant.setCategory(request.getCategory());
        if (request.getSubCategory() != null) merchant.setSubCategory(request.getSubCategory());
        if (request.getAddress() != null) merchant.setAddress(request.getAddress());
        if (request.getLat() != null) merchant.setLat(request.getLat());
        if (request.getLng() != null) merchant.setLng(request.getLng());
        if (request.getWhatsapp() != null) merchant.setWhatsapp(request.getWhatsapp());
        if (request.getPhone() != null) merchant.setPhone(request.getPhone());
        if (request.getEmail() != null) merchant.setEmail(request.getEmail());
        if (request.getSchedule() != null) merchant.setSchedule(request.getSchedule());

        return MerchantResponse.from(merchantRepo.save(merchant));
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");

        String slug = normalized;
        int counter = 1;
        while (merchantRepo.existsBySlug(slug)) {
            slug = normalized + "-" + counter++;
        }
        return slug;
    }
}
