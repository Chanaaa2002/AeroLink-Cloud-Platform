package com.aerolink.bookingservice.controller;

import com.aerolink.bookingservice.dto.CreateBookingRequest;
import com.aerolink.bookingservice.model.Booking;
import com.aerolink.bookingservice.service.BookingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final String internalPaymentKey;

    public BookingController(
            BookingService bookingService,
            @Value("${services.internal.payment-key}") String internalPaymentKey
    ) {
        this.bookingService = bookingService;

        if (internalPaymentKey == null || internalPaymentKey.isBlank()) {
            throw new IllegalStateException(
                    "PAYMENT_TO_BOOKING_INTERNAL_KEY is not configured."
            );
        }

        this.internalPaymentKey = internalPaymentKey;
    }

    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Booking> getBookingById(
            @PathVariable String bookingId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return bookingService.getBookingById(bookingId)
                .map(booking -> {
                    List<String> groups = jwt.getClaimAsStringList("cognito:groups");

                    boolean isStaff = groups != null && groups.contains("STAFF");
                    boolean isBookingOwner = jwt.getSubject().equals(booking.getUserId());

                    if (isStaff || isBookingOwner) {
                        return ResponseEntity.ok(booking);
                    }

                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .<Booking>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        request.setUserId(jwt.getSubject());

        Booking createdBooking = bookingService.createBooking(
                request,
                jwt.getTokenValue()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
    }

    /*
    * Backend-only operation used by Payment Service before creating a payment.
    * Payment Service must send the private internal key.
    */
    @GetMapping("/{bookingId}/internal-payment-view")
    public ResponseEntity<Booking> getBookingForPaymentService(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false)
            String providedInternalKey
    ) {
        if (!isValidInternalKey(providedInternalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return bookingService.getBookingById(bookingId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{bookingId}/payment-success")
    public ResponseEntity<Booking> confirmPaidBooking(
            @PathVariable String bookingId,
            @RequestHeader(value = "X-Internal-Service-Key", required = false)
            String providedInternalKey
    ) {
        if (!isValidInternalKey(providedInternalKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Booking confirmedBooking = bookingService.confirmPaidBooking(bookingId);
        return ResponseEntity.ok(confirmedBooking);
    }

    private boolean isValidInternalKey(String providedInternalKey) {
        if (providedInternalKey == null || providedInternalKey.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                internalPaymentKey.getBytes(StandardCharsets.UTF_8),
                providedInternalKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}