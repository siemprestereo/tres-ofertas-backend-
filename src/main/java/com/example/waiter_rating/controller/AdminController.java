package com.example.waiter_rating.controller;

import com.example.waiter_rating.dto.response.AdminRatingResponse;
import com.example.waiter_rating.dto.response.AdminStatsResponse;
import com.example.waiter_rating.dto.response.AdminUserResponse;
import com.example.waiter_rating.service.AppUserService;
import com.example.waiter_rating.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AppUserService userService;
    private final RatingService ratingService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(userService.listAllForAdmin());
    }

    @PatchMapping("/users/{id}/suspend")
    public ResponseEntity<Void> toggleSuspend(@PathVariable Long id) {
        userService.toggleSuspend(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ratings")
    public ResponseEntity<List<AdminRatingResponse>> listRatings() {
        return ResponseEntity.ok(ratingService.listAllForAdmin());
    }

    @DeleteMapping("/ratings/{id}")
    public ResponseEntity<Void> deleteRating(@PathVariable Long id) {
        ratingService.deleteByAdmin(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(userService.getAdminStats());
    }
}