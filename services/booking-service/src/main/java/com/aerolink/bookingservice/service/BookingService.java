package com.aerolink.bookingservice.service;

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

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Optional<Booking> getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId);
    }

    public Booking createBooking(Booking booking) {
        if (booking.getBookingId() == null || booking.getBookingId().isBlank()) {
            booking.setBookingId("BK-" + UUID.randomUUID());
        }

        booking.setBookingStatus("PENDING_PAYMENT");
        booking.setPaymentStatus("PENDING");
        booking.setCreatedAt(LocalDateTime.now().toString());

        return bookingRepository.save(booking);
    }
}