package com.aerolink.notificationservice.service;

import com.aerolink.notificationservice.model.Notification;
import com.aerolink.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> getNotificationsForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Authenticated user ID is required");
        }

        return notificationRepository.findByUserIdNewestFirst(userId);
    }
}