package com.aerolink.notificationservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${aws.dynamodb.notifications-table}")
    private String notificationsTable;

    @Value("${aws.dynamodb.notifications-user-index}")
    private String notificationsUserIndex;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new LinkedHashMap<>();

        response.put("service", "notification-service");
        response.put("status", "UP");
        response.put("table", notificationsTable);
        response.put("index", notificationsUserIndex);

        return ResponseEntity.ok(response);
    }
}