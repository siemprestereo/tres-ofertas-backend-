package com.tresofertas.controller;

import com.tresofertas.dto.response.MerchantResponse;
import com.tresofertas.service.FollowService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @PostMapping("/merchants/{merchantId}")
    public ResponseEntity<Map<String, String>> follow(@PathVariable Long merchantId,
                                                      HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        followService.follow(merchantId, userId);
        return ResponseEntity.ok(Map.of("message", "Comercio seguido"));
    }

    @DeleteMapping("/merchants/{merchantId}")
    public ResponseEntity<Map<String, String>> unfollow(@PathVariable Long merchantId,
                                                        HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        followService.unfollow(merchantId, userId);
        return ResponseEntity.ok(Map.of("message", "Dejaste de seguir el comercio"));
    }

    @GetMapping("/me")
    public ResponseEntity<List<MerchantResponse>> getFollowed(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(followService.getFollowed(userId));
    }

    @GetMapping("/merchants/{merchantId}/count")
    public ResponseEntity<Map<String, Long>> countFollowers(@PathVariable Long merchantId) {
        return ResponseEntity.ok(Map.of("followers", followService.countFollowers(merchantId)));
    }
}
