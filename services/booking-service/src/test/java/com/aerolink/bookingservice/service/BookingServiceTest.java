package com.aerolink.bookingservice.service;

import com.aerolink.bookingservice.client.FlightClient;
import com.aerolink.bookingservice.dto.CreateBookingRequest;
import com.aerolink.bookingservice.dto.FlightResponse;
import com.aerolink.bookingservice.model.Booking;
import com.aerolink.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FlightClient flightClient;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void getAllBookingsReturnsRepositoryResults() {
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        assertThat(bookingService.getAllBookings()).containsExactly(booking);
    }

    @Test
    void getMyBookingsRejectsBlankUserId() {
        assertThatThrownBy(() -> bookingService.getMyBookings("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated user ID is required.");
    }

    @Test
    void getMyBookingsDelegatesToRepository() {
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        when(bookingRepository.findByUserIdNewestFirst("user-1")).thenReturn(List.of(booking));

        assertThat(bookingService.getMyBookings("user-1")).containsExactly(booking);
    }

    @Test
    void createBookingBuildsPendingBookingWithCalculatedAmount() {
        CreateBookingRequest request = createRequest("user-1", "FL-1", "  Ada Lovelace  ", 2);
        FlightResponse flight = flightResponse(25000.0, 9);

        when(flightClient.getFlightById("FL-1", "token-123")).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(request, "token-123");

        assertThat(booking.getUserId()).isEqualTo("user-1");
        assertThat(booking.getFlightId()).isEqualTo("FL-1");
        assertThat(booking.getPassengerName()).isEqualTo("  Ada Lovelace  ");
        assertThat(booking.getSeatCount()).isEqualTo(2);
        assertThat(booking.getTotalAmount()).isEqualTo(50000.0);
        assertThat(booking.getBookingStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(booking.getPaymentStatus()).isEqualTo("PENDING");
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBookingRejectsMissingFlight() {
        CreateBookingRequest request = createRequest("user-1", "FL-1", "Ada Lovelace", 1);
        when(flightClient.getFlightById("FL-1", "token-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request, "token-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Flight not found: FL-1");
    }

    @Test
    void createBookingRejectsInsufficientSeats() {
        CreateBookingRequest request = createRequest("user-1", "FL-1", "Ada Lovelace", 3);
        FlightResponse flight = flightResponse(25000.0, 2);
        when(flightClient.getFlightById("FL-1", "token-123")).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.createBooking(request, "token-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Not enough seats available for flight FL-1");
    }

    @Test
    void createBookingRejectsBlankUserId() {
        CreateBookingRequest request = createRequest("  ", "FL-1", "Ada Lovelace", 1);

        assertThatThrownBy(() -> bookingService.createBooking(request, "token-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User ID is required.");
    }

    @Test
    void confirmPaidBookingCompletesTrustedBookingFlow() {
        Booking booking = sampleBooking("BK-1", "user-1", "PENDING_PAYMENT", "PENDING");
        booking.setFlightId("FL-1");
        booking.setSeatCount(2);
        when(bookingRepository.findById("BK-1")).thenReturn(Optional.of(booking));
        when(flightClient.reduceSeatsAfterConfirmedPayment("FL-1", 2))
                .thenReturn(flightResponse(25000.0, 8));
        when(bookingRepository.save(booking)).thenReturn(booking);

        Booking confirmed = bookingService.confirmPaidBooking("BK-1");

        assertThat(confirmed.getBookingStatus()).isEqualTo("CONFIRMED");
        assertThat(confirmed.getPaymentStatus()).isEqualTo("PAID");
        verify(flightClient).reduceSeatsAfterConfirmedPayment("FL-1", 2);
        verify(bookingRepository).save(booking);
    }

    @Test
    void confirmPaidBookingIsIdempotentForAlreadyConfirmedBooking() {
        Booking booking = sampleBooking("BK-1", "user-1", "CONFIRMED", "PAID");
        when(bookingRepository.findById("BK-1")).thenReturn(Optional.of(booking));

        Booking confirmed = bookingService.confirmPaidBooking("BK-1");

        assertThat(confirmed).isSameAs(booking);
        verify(flightClient, never()).reduceSeatsAfterConfirmedPayment("FL-1", 2);
        verify(bookingRepository, never()).save(booking);
    }

    @Test
    void confirmPaidBookingRejectsNonPendingBooking() {
        Booking booking = sampleBooking("BK-1", "user-1", "CONFIRMED", "PENDING");
        when(bookingRepository.findById("BK-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.confirmPaidBooking("BK-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking is not waiting for payment confirmation.");
    }

    @Test
    void confirmPaidBookingRejectsMissingBooking() {
        when(bookingRepository.findById("BK-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirmPaidBooking("BK-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking not found: BK-1");
    }

    private CreateBookingRequest createRequest(String userId, String flightId, String passengerName, int seatCount) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setUserId(userId);
        request.setFlightId(flightId);
        request.setPassengerName(passengerName);
        request.setSeatCount(seatCount);
        return request;
    }

    private FlightResponse flightResponse(double price, int availableSeats) {
        FlightResponse flight = new FlightResponse();
        flight.setFlightId("FL-1");
        flight.setFlightNumber("AL101");
        flight.setFromLocation("Colombo");
        flight.setToLocation("Dubai");
        flight.setDepartureTime("2026-06-05T08:00:00");
        flight.setArrivalTime("2026-06-05T12:00:00");
        flight.setPrice(price);
        flight.setAvailableSeats(availableSeats);
        flight.setStatus("SCHEDULED");
        return flight;
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