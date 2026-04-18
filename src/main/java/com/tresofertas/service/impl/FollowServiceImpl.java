package com.tresofertas.service.impl;

import com.tresofertas.dto.response.MerchantResponse;
import com.tresofertas.model.AppUser;
import com.tresofertas.model.Follow;
import com.tresofertas.model.Merchant;
import com.tresofertas.repository.AppUserRepo;
import com.tresofertas.repository.FollowRepo;
import com.tresofertas.repository.MerchantRepo;
import com.tresofertas.service.FollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FollowServiceImpl implements FollowService {

    private final FollowRepo followRepo;
    private final MerchantRepo merchantRepo;
    private final AppUserRepo userRepo;

    public FollowServiceImpl(FollowRepo followRepo, MerchantRepo merchantRepo, AppUserRepo userRepo) {
        this.followRepo = followRepo;
        this.merchantRepo = merchantRepo;
        this.userRepo = userRepo;
    }

    @Override
    public void follow(Long merchantId, Long userId) {
        if (followRepo.existsByConsumerIdAndMerchantId(userId, merchantId)) {
            throw new RuntimeException("Ya seguís este comercio");
        }

        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getRole() != AppUser.Role.CONSUMER) {
            throw new RuntimeException("Solo los consumidores pueden seguir comercios");
        }

        Merchant merchant = merchantRepo.findById(merchantId)
                .orElseThrow(() -> new RuntimeException("Comercio no encontrado"));

        Follow follow = new Follow();
        follow.setConsumer(user);
        follow.setMerchant(merchant);
        followRepo.save(follow);
    }

    @Override
    public void unfollow(Long merchantId, Long userId) {
        Follow follow = followRepo.findByConsumerIdAndMerchantId(userId, merchantId)
                .orElseThrow(() -> new RuntimeException("No seguís este comercio"));
        followRepo.delete(follow);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MerchantResponse> getFollowed(Long userId) {
        return followRepo.findByConsumerId(userId).stream()
                .map(f -> MerchantResponse.from(f.getMerchant()))
                .collect(Collectors.toList());
    }

    @Override
    public long countFollowers(Long merchantId) {
        return followRepo.countByMerchantId(merchantId);
    }
}
