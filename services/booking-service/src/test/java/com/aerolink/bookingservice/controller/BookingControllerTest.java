package com.aerolink.bookingservice.controller;

import com.aerolink.bookingservice.dto.CreateBookingRequest;
import com.aerolink.bookingservice.model.Booking;
import com.aerolink.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingControllerTest {

    private final BookingService bookingService = mock(BookingService.class);
    private final BookingController bookingController = new BookingController(
            bookingService,
            "payment-internal-key"
    );

    @Test
    void getAllBookingsReturnsBookings() {
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingService.getAllBookings()).thenReturn(List.of(booking));

        assertThat(bookingController.getAllBookings()).containsExactly(booking);
    }

    @Test
    void getMyBookingsUsesJwtSubject() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "token-123");
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingService.getMyBookings("user-1")).thenReturn(List.of(booking));

        assertThat(bookingController.getMyBookings(jwt)).containsExactly(booking);
    }

    @Test
    void getBookingByIdReturnsOkForOwner() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "token-123");
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingService.getBookingById("BK-1")).thenReturn(Optional.of(booking));

        ResponseEntity<Booking> response = bookingController.getBookingById("BK-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(booking);
    }

    @Test
    void getBookingByIdReturnsOkForStaff() {
        Jwt jwt = jwt("user-2", List.of("STAFF"), "token-123");
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingService.getBookingById("BK-1")).thenReturn(Optional.of(booking));

        ResponseEntity<Booking> response = bookingController.getBookingById("BK-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getBookingByIdReturnsForbiddenForNonOwnerPassenger() {
        Jwt jwt = jwt("user-2", List.of("PASSENGER"), "token-123");
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingService.getBookingById("BK-1")).thenReturn(Optional.of(booking));

        ResponseEntity<Booking> response = bookingController.getBookingById("BK-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getBookingByIdReturnsNotFoundWhenMissing() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "token-123");
        when(bookingService.getBookingById("BK-1")).thenReturn(Optional.empty());

        ResponseEntity<Booking> response = bookingController.getBookingById("BK-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createBookingDerivesUserIdFromJwtAndPassesToken() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"), "token-123");
        CreateBookingRequest request = new CreateBookingRequest();
        request.setUserId("manual-value");
        request.setFlightId("FL-1");
        request.setPassengerName("Ada Lovelace");
        request.setSeatCount(2);

        Booking created = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingService.createBooking(eq(request), eq("token-123"))).thenReturn(created);

        ResponseEntity<Booking> response = bookingController.createBooking(request, jwt);

        assertThat(request.getUserId()).isEqualTo("user-1");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);
        verify(bookingService).createBooking(eq(request), eq("token-123"));
    }

    @Test
    void getBookingForPaymentServiceRejectsInvalidInternalKey() {
        ResponseEntity<Booking> response = bookingController.getBookingForPaymentService(
                "BK-1",
                "wrong-key"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getBookingForPaymentServiceReturnsBookingForValidInternalKey() {
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingService.getBookingById("BK-1")).thenReturn(Optional.of(booking));

        ResponseEntity<Booking> response = bookingController.getBookingForPaymentService(
                "BK-1",
                "payment-internal-key"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(booking);
    }

    @Test
    void confirmPaidBookingRejectsInvalidInternalKey() {
        ResponseEntity<Booking> response = bookingController.confirmPaidBooking(
                "BK-1",
                "wrong-key"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void confirmPaidBookingCallsServiceForValidInternalKey() {
        Booking booking = sampleBooking("BK-1", "user-1", "CONFIRMED", "PAID");
        when(bookingService.confirmPaidBooking("BK-1")).thenReturn(booking);

        ResponseEntity<Booking> response = bookingController.confirmPaidBooking(
                "BK-1",
                "payment-internal-key"
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(booking);
    }

    private Jwt jwt(String subject, List<String> groups, String tokenValue) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(groups);
        when(jwt.getTokenValue()).thenReturn(tokenValue);
        return jwt;
    }

    private Booking sampleBooking(String bookingId, String userId, String bookingStatus, String paymentStatus) {
        Booking booking = new Booking();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setFlightId("FL-1");
        booking.setPassengerName("Ada Lovelace");
        booking.setSeatCount(2);
        booking.setTotalAmount(50000.0);
        booking.setBookingStatus(bookingStatus);
        booking.setPaymentStatus(paymentStatus);
        booking.setCreatedAt("2026-06-05T08:00:00");
        return booking;
    }
}