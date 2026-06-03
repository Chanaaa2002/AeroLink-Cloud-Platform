package com.aerolink.flightservice.controller;

import com.aerolink.flightservice.model.Flight;
import com.aerolink.flightservice.service.FlightService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;
    private final String internalBookingKey;

    public FlightController(
            FlightService flightService,
            @Value("${services.internal.booking-key}") String internalBookingKey
    ) {
        this.flightService = flightService;

        if (internalBookingKey == null || internalBookingKey.isBlank()) {
            throw new IllegalStateException(
                    "BOOKING_TO_FLIGHT_INTERNAL_KEY is not configured."
            );
        }

        this.internalBookingKey = internalBookingKey;
    }

    @GetMapping
    public List<Flight> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{flightId}")
    public ResponseEntity<Flight> getFlightById(@PathVariable String flightId) {
        return flightService.getFlightById(flightId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Flight createFlight(@RequestBody Flight flight) {
        return flightService.createFlight(flight);
    }

    @PutMapping("/{flightId}")
    public ResponseEntity<Flight> updateFlight(
            @PathVariable String flightId,
            @RequestBody Flight updatedFlight
    ) {
        return flightService.updateFlight(flightId, updatedFlight)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * STAFF operation used for normal flight administration.
     */
    @PutMapping("/{flightId}/seats")
    public ResponseEntity<Flight> updateAvailableSeats(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> request
    ) {
        Object seatsValue = request.get("availableSeats");

        if (seatsValue == null) {
            return ResponseEntity.badRequest().build();
        }

        int availableSeats = ((Number) seatsValue).intValue();

        return flightService.updateAvailableSeats(flightId, availableSeats)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{flightId}/price")
    public ResponseEntity<Flight> updatePrice(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> request
    ) {
        Object priceValue = request.get("price");

        if (priceValue == null) {
            return ResponseEntity.badRequest().build();
        }

        double price = ((Number) priceValue).doubleValue();

        return flightService.updatePrice(flightId, price)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * Backend-only operation used after verified payment confirmation.
     * Booking Service sends the internal private key.
     * Passengers and the React frontend must never receive this key.
     */
    @PutMapping("/{flightId}/internal-seat-reduction")
    public ResponseEntity<Flight> reduceSeatsAfterConfirmedPayment(
            @PathVariable String flightId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false)
            String providedInternalKey,
            @RequestBody Map<String, Object> request
    ) {
        if (!isValidInternalKey(providedInternalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Object seatCountValue = request.get("seatCount");

        if (seatCountValue == null) {
            return ResponseEntity.badRequest().build();
        }

        int seatCount = ((Number) seatCountValue).intValue();

        if (seatCount <= 0) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Flight> existingFlight = flightService.getFlightById(flightId);

        if (existingFlight.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Flight flight = existingFlight.get();

        if (flight.getAvailableSeats() < seatCount) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        int remainingSeats = flight.getAvailableSeats() - seatCount;

        return flightService.updateAvailableSeats(flightId, remainingSeats)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private boolean isValidInternalKey(String providedInternalKey) {
        if (providedInternalKey == null || providedInternalKey.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                internalBookingKey.getBytes(StandardCharsets.UTF_8),
                providedInternalKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
