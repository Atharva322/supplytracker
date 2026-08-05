package com.agri.supplytracker.service;

import com.agri.supplytracker.model.Notification;
import com.agri.supplytracker.repository.NotificationRepository;
import com.agri.supplytracker.repository.UserRepository;
import com.agri.supplytracker.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void notifyAdmins(String type, String title, String message, String productId, 
                            String productName, String triggeredBy, String triggeredByUser) {
        
        List<User> admins = userRepository.findByRole("ROLE_ADMIN");

        for (User admin : admins) {
            Notification notification = new Notification();
            notification.setRecipientId(admin.getUsername());
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setProductId(productId);
            notification.setProductName(productName);
            notification.setTriggeredBy(triggeredBy);
            notification.setRead(false);
            notification.setCreatedAt(LocalDateTime.now());

            Notification saved = notificationRepository.save(notification);

            messagingTemplate.convertAndSendToUser(
                admin.getUsername(),
                "/queue/notifications",
                saved
            );
        }
    }

    public void notifyUsers(Collection<String> usernames, String type, String title, String message, String resourceId) {
        usernames.stream().distinct().forEach(username -> {
            Notification notification = new Notification();
            notification.setRecipientId(username); notification.setType(type); notification.setTitle(title);
            notification.setMessage(message); notification.setProductId(resourceId); notification.setCreatedAt(LocalDateTime.now());
            Notification saved = notificationRepository.save(notification);
            Runnable deliver = () -> messagingTemplate.convertAndSendToUser(username, "/queue/notifications", saved);
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() { deliver.run(); }
                });
            } else {
                deliver.run();
            }
        });
    }

    public List<Notification> getNotifications(String userId) {
        return notificationRepository.findByRecipientId(userId);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    public Notification markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, userId).orElse(null);
        if (notification != null) {
            notification.setRead(true);
            return notificationRepository.save(notification);
        }
        return null;
    }

    public void markAllAsRead(String userId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalse(userId);
        unread.forEach(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public boolean deleteNotification(String notificationId, String userId) {
        return notificationRepository.findByIdAndRecipientId(notificationId, userId)
            .map(notification -> {
                notificationRepository.delete(notification);
                return true;
            })
            .orElse(false);
    }
}
