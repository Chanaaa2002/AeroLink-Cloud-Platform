package com.aerolink.notificationservice.controller;

import com.aerolink.notificationservice.model.Notification;
import com.aerolink.notificationservice.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<Notification>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt) {

        String authenticatedUserId = jwt.getSubject();

        List<Notification> notifications =
                notificationService.getNotificationsForUser(authenticatedUserId);

        return ResponseEntity.ok(notifications);
    }
}