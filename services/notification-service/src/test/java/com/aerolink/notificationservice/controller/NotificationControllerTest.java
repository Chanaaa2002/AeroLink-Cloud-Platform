package com.aerolink.notificationservice.controller;

import com.aerolink.notificationservice.model.Notification;
import com.aerolink.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationControllerTest {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationController notificationController = new NotificationController(notificationService);

    @Test
    void getMyNotificationsUsesJwtSubject() {
        Jwt jwt = jwt("user-1");
        Notification notification = sampleNotification("NT-1", "user-1", "PAYMENT_SUCCESS");
        when(notificationService.getNotificationsForUser("user-1")).thenReturn(List.of(notification));

        ResponseEntity<List<Notification>> response = notificationController.getMyNotifications(jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(notification);
        verify(notificationService).getNotificationsForUser("user-1");
    }

    private Jwt jwt(String subject) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        return jwt;
    }

    private Notification sampleNotification(String notificationId, String userId, String type) {
        Notification notification = new Notification();
        notification.setNotificationId(notificationId);
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle("Travel update");
        notification.setMessage("Your journey has been updated.");
        notification.setStatus("UNREAD");
        notification.setCreatedAt("2026-06-05T08:00:00");
        return notification;
    }
}