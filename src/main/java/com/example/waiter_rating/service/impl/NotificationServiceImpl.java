package com.example.waiter_rating.service.impl;

import com.example.waiter_rating.model.AppUser;
import com.example.waiter_rating.model.Notification;
import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.repository.AppUserRepo;
import com.example.waiter_rating.repository.NotificationRepo;
import com.example.waiter_rating.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepo notificationRepo;
    private final AppUserRepo appUserRepo;

    @Override
    public List<Map<String, Object>> getForUser(Long userId) {
        return notificationRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(n -> Map.<String, Object>of(
                        "id", n.getId(),
                        "title", n.getTitle(),
                        "message", n.getMessage(),
                        "isRead", n.getIsRead(),
                        "createdAt", n.getCreatedAt().toString()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        notificationRepo.markAllReadByUserId(userId);
    }

    @Override
    @Transactional
    public void markOneRead(Long notificationId, Long userId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setIsRead(true);
                notificationRepo.save(n);
            }
        });
    }

    @Override
    @Transactional
    public void sendToUser(Long userId, String title, String message) {
        AppUser user = appUserRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        notificationRepo.save(Notification.builder()
                .user(user).title(title).message(message).build());
    }

    @Override
    @Transactional
    public void sendToAll(String title, String message) {
        appUserRepo.findAll().forEach(user ->
                notificationRepo.save(Notification.builder()
                        .user(user).title(title).message(message).build()));
    }

    @Override
    @Transactional
    public void sendToRole(UserRole role, String title, String message) {
        appUserRepo.findAll().stream()
                .filter(u -> u.getActiveRole() == role)
                .forEach(user ->
                        notificationRepo.save(Notification.builder()
                                .user(user).title(title).message(message).build()));
    }
}
