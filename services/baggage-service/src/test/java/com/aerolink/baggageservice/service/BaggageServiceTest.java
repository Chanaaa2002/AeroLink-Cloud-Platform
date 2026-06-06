package com.aerolink.baggageservice.service;

import com.aerolink.baggageservice.client.BookingClient;
import com.aerolink.baggageservice.dto.BookingResponse;
import com.aerolink.baggageservice.dto.CreateBaggageRequest;
import com.aerolink.baggageservice.dto.UpdateBaggageStatusRequest;
import com.aerolink.baggageservice.event.BaggageEventPublisher;
import com.aerolink.baggageservice.model.Baggage;
import com.aerolink.baggageservice.repository.BaggageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaggageServiceTest {

    @Mock
    private BaggageRepository baggageRepository;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private BaggageEventPublisher baggageEventPublisher;

    @InjectMocks
    private BaggageService baggageService;

    @Test
    void getBaggageByBookingIdReturnsRepositoryResults() {
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "CHECKED_IN");
        when(bookingClient.getBookingById("BK-1", "token-123")).thenReturn(Optional.of(bookingResponse("BK-1", "user-1", "CONFIRMED", "PAID")));
        when(baggageRepository.findByBookingId("BK-1")).thenReturn(List.of(baggage));

        assertThat(baggageService.getBaggageByBookingId("BK-1", "token-123")).containsExactly(baggage);
    }

    @Test
    void getBaggageByBookingIdRejectsBlankBookingId() {
        assertThatThrownBy(() -> baggageService.getBaggageByBookingId("  ", "token-123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking ID is required.");
    }

    @Test
    void getBaggageByBookingIdRejectsBlankToken() {
        assertThatThrownBy(() -> baggageService.getBaggageByBookingId("BK-1", "  "))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException statusException = (ResponseStatusException) exception;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    void getBaggageByBookingIdRejectsMissingBooking() {
        when(bookingClient.getBookingById("BK-1", "token-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baggageService.getBaggageByBookingId("BK-1", "token-123"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> {
                    ResponseStatusException statusException = (ResponseStatusException) exception;
                    assertThat(statusException.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                });
    }

    @Test
    void createBaggageUsesDefaultLocationWhenBlank() {
        CreateBaggageRequest request = new CreateBaggageRequest();
        request.setBookingId("BK-1");
        request.setCurrentLocation("  ");

        when(bookingClient.getBookingById("BK-1", "staff-token"))
                .thenReturn(Optional.of(bookingResponse("BK-1", "user-1", "CONFIRMED", "PAID")));
        when(baggageRepository.save(any(Baggage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Baggage baggage = baggageService.createBaggage(request, "staff-token");

        assertThat(baggage.getBookingId()).isEqualTo("BK-1");
        assertThat(baggage.getUserId()).isEqualTo("user-1");
        assertThat(baggage.getStatus()).isEqualTo("CHECKED_IN");
        assertThat(baggage.getCurrentLocation()).isEqualTo("Colombo Check-in Counter");
        verify(baggageRepository).save(any(Baggage.class));
    }

    @Test
    void createBaggageRejectsUnpaidBooking() {
        CreateBaggageRequest request = new CreateBaggageRequest();
        request.setBookingId("BK-1");

        when(bookingClient.getBookingById("BK-1", "staff-token"))
                .thenReturn(Optional.of(bookingResponse("BK-1", "user-1", "PENDING_PAYMENT", "PENDING")));

        assertThatThrownBy(() -> baggageService.createBaggage(request, "staff-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Baggage can only be created for a confirmed and paid booking.");
    }

    @Test
    void createBaggageRejectsMissingTrustedOwner() {
        CreateBaggageRequest request = new CreateBaggageRequest();
        request.setBookingId("BK-1");

        when(bookingClient.getBookingById("BK-1", "staff-token"))
                .thenReturn(Optional.of(bookingResponse("BK-1", "  ", "CONFIRMED", "PAID")));

        assertThatThrownBy(() -> baggageService.createBaggage(request, "staff-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking does not contain a verified passenger owner.");
    }

    @ParameterizedTest
    @CsvSource({
            "CHECKED_IN,LOADED",
            "CHECKED_IN,DELAYED",
            "LOADED,IN_TRANSIT",
            "LOADED,DELAYED",
            "IN_TRANSIT,ARRIVED",
            "IN_TRANSIT,DELAYED",
            "ARRIVED,COLLECTED",
            "DELAYED,LOADED",
            "DELAYED,IN_TRANSIT",
            "DELAYED,ARRIVED"
    })
    void updateBaggageStatusAllowsValidTransitions(String currentStatus, String nextStatus) {
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", currentStatus);
        when(baggageRepository.findById("BAG-1")).thenReturn(Optional.of(baggage));
        when(baggageRepository.save(baggage)).thenAnswer(invocation -> invocation.getArgument(0));

        Baggage updated = baggageService.updateBaggageStatus("BAG-1", updateStatus(nextStatus, "Transfer Handling Area"));

        assertThat(updated.getStatus()).isEqualTo(nextStatus);
        assertThat(updated.getCurrentLocation()).isEqualTo("Transfer Handling Area");
        InOrder inOrder = inOrder(baggageRepository, baggageEventPublisher);
        inOrder.verify(baggageRepository).save(baggage);
        inOrder.verify(baggageEventPublisher).publishStatusUpdated(baggage);
    }

    @Test
    void updateBaggageStatusReturnsExistingBaggageForSameStatus() {
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "LOADED");
        when(baggageRepository.findById("BAG-1")).thenReturn(Optional.of(baggage));

        Baggage updated = baggageService.updateBaggageStatus("BAG-1", updateStatus("loaded", "Sorting Area"));

        assertThat(updated).isSameAs(baggage);
        verify(baggageRepository, never()).save(any());
        verify(baggageEventPublisher, never()).publishStatusUpdated(any());
    }

    @Test
    void updateBaggageStatusRejectsInvalidTransition() {
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "ARRIVED");
        when(baggageRepository.findById("BAG-1")).thenReturn(Optional.of(baggage));

        assertThatThrownBy(() -> baggageService.updateBaggageStatus("BAG-1", updateStatus("LOADED", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid baggage movement from ARRIVED to LOADED.");
    }

    @Test
    void updateBaggageStatusRejectsUnknownStatus() {
        Baggage baggage = sampleBaggage("BAG-1", "BK-1", "user-1", "CHECKED_IN");
        when(baggageRepository.findById("BAG-1")).thenReturn(Optional.of(baggage));

        assertThatThrownBy(() -> baggageService.updateBaggageStatus("BAG-1", updateStatus("ARCHIVED", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid baggage status. Use CHECKED_IN, LOADED, IN_TRANSIT, ARRIVED, COLLECTED or DELAYED.");
    }

    private BookingResponse bookingResponse(String bookingId, String userId, String bookingStatus, String paymentStatus) {
        BookingResponse booking = new BookingResponse();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setBookingStatus(bookingStatus);
        booking.setPaymentStatus(paymentStatus);
        return booking;
    }

    private UpdateBaggageStatusRequest updateStatus(String status, String currentLocation) {
        UpdateBaggageStatusRequest request = new UpdateBaggageStatusRequest();
        request.setStatus(status);
        request.setCurrentLocation(currentLocation);
        return request;
    }

    private Baggage sampleBaggage(String baggageId, String bookingId, String userId, String status) {
        Baggage baggage = new Baggage();
        baggage.setBaggageId(baggageId);
        baggage.setBookingId(bookingId);
        baggage.setUserId(userId);
        baggage.setTagNumber("TAG-1");
        baggage.setStatus(status);
        baggage.setCurrentLocation("Check-in Counter");
        baggage.setLastUpdated("2026-06-05T08:00:00");
        return baggage;
    }
}