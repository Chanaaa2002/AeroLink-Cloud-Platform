package com.aerolink.bookingservice.service;

import com.aerolink.bookingservice.client.FlightClient;
import com.aerolink.bookingservice.dto.CreateBookingRequest;
import com.aerolink.bookingservice.dto.FlightResponse;
import com.aerolink.bookingservice.model.Booking;
import com.aerolink.bookingservice.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightClient flightClient;

    public BookingService(BookingRepository bookingRepository, FlightClient flightClient) {
        this.bookingRepository = bookingRepository;
        this.flightClient = flightClient;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId);
    }

    public Booking createBooking(CreateBookingRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("User ID is required.");
        }

        if (request.getFlightId() == null || request.getFlightId().isBlank()) {
            throw new IllegalArgumentException("Flight ID is required.");
        }

        if (request.getPassengerName() == null || request.getPassengerName().isBlank()) {
            throw new IllegalArgumentException("Passenger name is required.");
        }

        if (request.getSeatCount() <= 0) {
            throw new IllegalArgumentException("Seat count must be greater than zero.");
        }

        FlightResponse flight = flightClient.getFlightById(request.getFlightId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flight not found: " + request.getFlightId()
                ));

        if (flight.getAvailableSeats() < request.getSeatCount()) {
            throw new IllegalArgumentException(
                    "Not enough seats available for flight " + request.getFlightId()
            );
        }

        double calculatedTotalAmount = flight.getPrice() * request.getSeatCount();

        Booking booking = new Booking();
        booking.setBookingId("BK-" + UUID.randomUUID());
        booking.setUserId(request.getUserId());
        booking.setFlightId(request.getFlightId());
        booking.setPassengerName(request.getPassengerName());
        booking.setSeatCount(request.getSeatCount());
        booking.setTotalAmount(calculatedTotalAmount);
        booking.setBookingStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("PENDING");
        booking.setCreatedAt(LocalDateTime.now().toString());

        return bookingRepository.save(booking);
    }
}