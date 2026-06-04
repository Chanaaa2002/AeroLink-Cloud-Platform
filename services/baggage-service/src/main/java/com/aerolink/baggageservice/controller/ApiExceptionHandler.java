package com.aerolink.baggageservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST",
                "message", exception.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleServiceUnavailable(
            IllegalStateException exception
    ) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "SERVICE_UNAVAILABLE",
                "message", exception.getMessage()
        ));
    }

    /*
     * Used for secure access-control responses raised after authentication.
     * For example, a passenger attempting to read baggage through
     * a booking that does not belong to them must receive 403 Forbidden.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(
            ResponseStatusException exception
    ) {
        String errorName = HttpStatus
                .valueOf(exception.getStatusCode().value())
                .name();

        String message = exception.getReason() != null
                ? exception.getReason()
                : "Request was rejected.";

        return ResponseEntity.status(exception.getStatusCode()).body(Map.of(
                "error", errorName,
                "message", message
        ));
    }
}