package com.example.waiter_rating.service;

import com.example.waiter_rating.dto.response.AdminStatsResponse;
import com.example.waiter_rating.dto.response.AdminUserResponse;
import com.example.waiter_rating.dto.response.AppUserResponse;
import com.example.waiter_rating.model.AppUser;

import java.util.List;
import java.util.Map;

public interface AppUserService {

    AppUserResponse getById(Long id);
    List<AppUserResponse> listAll();
    Map<String, Object> checkUserRoles(String authHeader);
    void createVerificationToken(AppUser user);
    boolean verifyEmail(String token);
    void requestPasswordReset(String email);
    boolean resetPassword(String token, String newPassword);
    void sendWelcomeEmail(AppUser user);
    void updateProfilePicture(Long userId, String photoUrl);

    // Admin
    List<AdminUserResponse> listAllForAdmin();
    void toggleSuspend(Long id);
    AdminStatsResponse getAdminStats();
}