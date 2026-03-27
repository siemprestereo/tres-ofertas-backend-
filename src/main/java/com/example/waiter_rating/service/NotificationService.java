package com.example.waiter_rating.service;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.UserRole;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    List<Map<String, Object>> getForUser(Long userId);
    long getUnreadCount(Long userId);
    void markAllRead(Long userId);
    void markOneRead(Long notificationId, Long userId);
    void sendToUser(Long userId, String title, String message);
    void sendToAll(String title, String message);
    void sendToRole(UserRole role, String title, String message);
}
