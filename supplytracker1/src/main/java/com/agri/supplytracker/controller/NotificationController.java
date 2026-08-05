package com.agri.supplytracker.controller;

import com.agri.supplytracker.model.Notification;
import com.agri.supplytracker.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        String userId = authentication.getName();
        List<Notification> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        String userId = authentication.getName();
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String notificationId, Authentication authentication) {
        Notification notification = notificationService.markAsRead(notificationId, authentication.getName());
        if (notification == null) {
            return ResponseEntity.status(404).body("Notification not found");
        }
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("User not authenticated");
        }
        String userId = authentication.getName();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable String notificationId, Authentication authentication) {
        if (!notificationService.deleteNotification(notificationId, authentication.getName())) {
            return ResponseEntity.status(404).body(Map.of("message", "Notification not found"));
        }
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }
}
