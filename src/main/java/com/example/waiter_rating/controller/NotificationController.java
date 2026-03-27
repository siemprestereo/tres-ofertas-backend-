package com.example.waiter_rating.controller;

import com.example.waiter_rating.model.UserRole;
import com.example.waiter_rating.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getMyNotifications(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(notificationService.getForUser(userId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        notificationService.markAllRead(userId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markOneRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();
        notificationService.markOneRead(id, userId);
        return ResponseEntity.ok().build();
    }

    // ===== ADMIN =====

    @PostMapping("/admin/send")
    public ResponseEntity<?> adminSend(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String role = (String) request.getAttribute("userType");
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).build();

        String title = body.get("title");
        String message = body.get("message");
        String target = body.getOrDefault("target", "ALL"); // ALL | PROFESSIONAL | CLIENT | USER:{id}

        if (title == null || message == null)
            return ResponseEntity.badRequest().body(Map.of("error", "title y message son requeridos"));

        if (target.startsWith("USER:")) {
            Long userId = Long.parseLong(target.split(":")[1]);
            notificationService.sendToUser(userId, title, message);
        } else if ("PROFESSIONAL".equals(target)) {
            notificationService.sendToRole(UserRole.PROFESSIONAL, title, message);
        } else if ("CLIENT".equals(target)) {
            notificationService.sendToRole(UserRole.CLIENT, title, message);
        } else {
            notificationService.sendToAll(title, message);
        }

        return ResponseEntity.ok(Map.of("message", "Notificación enviada"));
    }
}
