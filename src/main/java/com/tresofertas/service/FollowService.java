package com.tresofertas.service;

import com.tresofertas.dto.response.MerchantResponse;

import java.util.List;

public interface FollowService {
    void follow(Long merchantId, Long userId);
    void unfollow(Long merchantId, Long userId);
    List<MerchantResponse> getFollowed(Long userId);
    long countFollowers(Long merchantId);
}
