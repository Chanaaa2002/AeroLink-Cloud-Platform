package com.aerolink.baggageservice.service;

import com.aerolink.baggageservice.client.BookingClient;
import com.aerolink.baggageservice.dto.BookingResponse;
import com.aerolink.baggageservice.dto.CreateBaggageRequest;
import com.aerolink.baggageservice.dto.UpdateBaggageStatusRequest;
import com.aerolink.baggageservice.model.Baggage;
import com.aerolink.baggageservice.repository.BaggageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class BaggageService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "CHECKED_IN", Set.of("LOADED", "DELAYED"),
            "LOADED", Set.of("IN_TRANSIT", "DELAYED"),
            "IN_TRANSIT", Set.of("ARRIVED", "DELAYED"),
            "ARRIVED", Set.of("COLLECTED"),
            "DELAYED", Set.of("LOADED", "IN_TRANSIT", "ARRIVED"),
            "COLLECTED", Set.of()
    );

    private final BaggageRepository baggageRepository;
    private final BookingClient bookingClient;

    public BaggageService(
            BaggageRepository baggageRepository,
            BookingClient bookingClient
    ) {
        this.baggageRepository = baggageRepository;
        this.bookingClient = bookingClient;
    }

    public Optional<Baggage> getBaggageById(String baggageId) {
        return baggageRepository.findById(baggageId);
    }

    /*
     * Secure tracking lookup by booking ID.
     *
     * The caller's verified Cognito token is forwarded to Booking Service first:
     * - STAFF may inspect baggage for any valid booking.
     * - PASSENGER may inspect baggage only for their own booking.
     * - An empty baggage list is returned only after booking access is validated.
     */
    public List<Baggage> getBaggageByBookingId(
            String bookingId,
            String accessToken
    ) {
        if (bookingId == null || bookingId.isBlank()) {
            throw new IllegalArgumentException("Booking ID is required.");
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated access token is required."
            );
        }

        BookingResponse booking = bookingClient.getBookingById(
                        bookingId,
                        accessToken
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found."
                ));

        return baggageRepository.findByBookingId(booking.getBookingId());
    }

    /*
     * STAFF-only baggage creation flow.
     * Baggage Service forwards the verified STAFF token to Booking Service
     * so it can confirm that the booking is paid and retrieve its trusted owner.
     */
    public Baggage createBaggage(
            CreateBaggageRequest request,
            String staffAccessToken
    ) {
        if (staffAccessToken == null || staffAccessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated staff token is required."
            );
        }

        if (request.getBookingId() == null || request.getBookingId().isBlank()) {
            throw new IllegalArgumentException("Booking ID is required.");
        }

        BookingResponse booking = bookingClient.getBookingById(
                        request.getBookingId(),
                        staffAccessToken
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + request.getBookingId()
                ));

        if (!"CONFIRMED".equals(booking.getBookingStatus())
                || !"PAID".equals(booking.getPaymentStatus())) {
            throw new IllegalArgumentException(
                    "Baggage can only be created for a confirmed and paid booking."
            );
        }

        /*
         * New secure baggage records must be connected to a trusted
         * Cognito passenger owner.
         */
        if (booking.getUserId() == null || booking.getUserId().isBlank()) {
            throw new IllegalArgumentException(
                    "Booking does not contain a verified passenger owner."
            );
        }

        String location = request.getCurrentLocation();

        if (location == null || location.isBlank()) {
            location = "Colombo Check-in Counter";
        }

        String shortTag = UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();

        Baggage baggage = new Baggage();
        baggage.setBaggageId("BAG-" + UUID.randomUUID());
        baggage.setBookingId(booking.getBookingId());
        baggage.setUserId(booking.getUserId());
        baggage.setTagNumber("TAG-AERO-" + shortTag);
        baggage.setStatus("CHECKED_IN");
        baggage.setCurrentLocation(location);
        baggage.setLastUpdated(LocalDateTime.now().toString());

        return baggageRepository.save(baggage);
    }

    public Baggage updateBaggageStatus(
            String baggageId,
            UpdateBaggageStatusRequest request
    ) {
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            throw new IllegalArgumentException("New baggage status is required.");
        }

        Baggage baggage = baggageRepository.findById(baggageId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Baggage not found: " + baggageId
                ));

        String currentStatus = baggage.getStatus();
        String newStatus = request.getStatus().trim().toUpperCase();

        if (!ALLOWED_TRANSITIONS.containsKey(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid baggage status. Use CHECKED_IN, LOADED, IN_TRANSIT, ARRIVED, COLLECTED or DELAYED."
            );
        }

        if (currentStatus.equals(newStatus)) {
            return baggage;
        }

        if (!ALLOWED_TRANSITIONS
                .getOrDefault(currentStatus, Set.of())
                .contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid baggage movement from " + currentStatus + " to " + newStatus + "."
            );
        }

        baggage.setStatus(newStatus);

        if (request.getCurrentLocation() != null
                && !request.getCurrentLocation().isBlank()) {
            baggage.setCurrentLocation(request.getCurrentLocation());
        }

        baggage.setLastUpdated(LocalDateTime.now().toString());

        return baggageRepository.save(baggage);
    }
}