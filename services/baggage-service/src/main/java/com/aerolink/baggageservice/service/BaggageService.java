package com.aerolink.baggageservice.service;

import com.aerolink.baggageservice.client.BookingClient;
import com.aerolink.baggageservice.dto.BookingResponse;
import com.aerolink.baggageservice.dto.CreateBaggageRequest;
import com.aerolink.baggageservice.model.Baggage;
import com.aerolink.baggageservice.repository.BaggageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class BaggageService {

    private final BaggageRepository baggageRepository;
    private final BookingClient bookingClient;

    public BaggageService(BaggageRepository baggageRepository, BookingClient bookingClient) {
        this.baggageRepository = baggageRepository;
        this.bookingClient = bookingClient;
    }

    public Optional<Baggage> getBaggageById(String baggageId) {
        return baggageRepository.findById(baggageId);
    }

    public Baggage createBaggage(CreateBaggageRequest request) {
        if (request.getBookingId() == null || request.getBookingId().isBlank()) {
            throw new IllegalArgumentException("Booking ID is required.");
        }

        BookingResponse booking = bookingClient.getBookingById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + request.getBookingId()
                ));

        if (!"CONFIRMED".equals(booking.getBookingStatus())
                || !"PAID".equals(booking.getPaymentStatus())) {
            throw new IllegalArgumentException(
                    "Baggage can only be created for a confirmed and paid booking."
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
        baggage.setTagNumber("TAG-AERO-" + shortTag);
        baggage.setStatus("CHECKED_IN");
        baggage.setCurrentLocation(location);
        baggage.setLastUpdated(LocalDateTime.now().toString());

        return baggageRepository.save(baggage);
    }
}