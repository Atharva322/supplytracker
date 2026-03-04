package com.agri.supplytracker.repository;

import com.agri.supplytracker.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(String recipientId);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    List<Notification> findByRecipientId(String recipientId);
    List<Notification> findByRecipientIdAndReadFalse(String recipientId);
    long countByRecipientIdAndReadFalse(String recipientId);
}