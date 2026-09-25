package com.smartdesk.service;

import com.smartdesk.entity.Notification;
import com.smartdesk.entity.Ticket;
import com.smartdesk.entity.User;
import com.smartdesk.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification createNotification(User user, Ticket ticket, String type, String title, String message) {
        Notification notification = new Notification(user, ticket, type, title, message);
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            n.setReadAt(OffsetDateTime.now());
            notificationRepository.save(n);
        });
    }
}
