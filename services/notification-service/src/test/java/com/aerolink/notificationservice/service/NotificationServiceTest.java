package com.aerolink.notificationservice.service;

import com.aerolink.notificationservice.model.Notification;
import com.aerolink.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getNotificationsForUserReturnsRepositoryResults() {
        Notification notification = sampleNotification("NT-1", "user-1", "PAYMENT_SUCCESS");
        when(notificationRepository.findByUserIdNewestFirst("user-1")).thenReturn(List.of(notification));

        assertThat(notificationService.getNotificationsForUser("user-1")).containsExactly(notification);
    }

    @Test
    void getNotificationsForUserRejectsBlankUserId() {
        assertThatThrownBy(() -> notificationService.getNotificationsForUser("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated user ID is required");
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