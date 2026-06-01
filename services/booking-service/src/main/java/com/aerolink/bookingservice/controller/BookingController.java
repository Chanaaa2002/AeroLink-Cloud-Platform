package com.aerolink.bookingservice.controller;

import com.aerolink.bookingservice.dto.CreateBookingRequest;
import com.aerolink.bookingservice.model.Booking;
import com.aerolink.bookingservice.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBookingById(@PathVariable String bookingId) {
        return bookingService.getBookingById(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody CreateBookingRequest request) {
        Booking createdBooking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
    }

    @PutMapping("/{bookingId}/payment-success")
    public ResponseEntity<Booking> confirmPaidBooking(
            @PathVariable String bookingId
    ) {
        Booking confirmedBooking = bookingService.confirmPaidBooking(bookingId);
        return ResponseEntity.ok(confirmedBooking);
    }
}