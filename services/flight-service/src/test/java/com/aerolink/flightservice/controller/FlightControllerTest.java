package com.aerolink.flightservice.controller;

import com.aerolink.flightservice.model.Flight;
import com.aerolink.flightservice.service.FlightService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlightControllerTest {

    private final FlightService flightService = mock(FlightService.class);
    private final FlightController flightController = new FlightController(
            flightService,
            "booking-internal-key"
    );

    @Test
    void getAllFlightsReturnsFlights() {
        Flight flight = sampleFlight("FL-1", 40, 25000.0);
        when(flightService.getAllFlights()).thenReturn(List.of(flight));

        assertThat(flightController.getAllFlights()).containsExactly(flight);
    }

    @Test
    void getFlightByIdReturnsOkWhenFound() {
        Flight flight = sampleFlight("FL-1", 40, 25000.0);
        when(flightService.getFlightById("FL-1")).thenReturn(Optional.of(flight));

        ResponseEntity<Flight> response = flightController.getFlightById("FL-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(flight);
    }

    @Test
    void getFlightByIdReturnsNotFoundWhenMissing() {
        when(flightService.getFlightById("FL-1")).thenReturn(Optional.empty());

        ResponseEntity<Flight> response = flightController.getFlightById("FL-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createFlightDelegatesToService() {
        Flight flight = sampleFlight("FL-1", 40, 25000.0);
        when(flightService.createFlight(flight)).thenReturn(flight);

        assertThat(flightController.createFlight(flight)).isSameAs(flight);
    }

    @Test
    void updateFlightReturnsNotFoundWhenServiceHasNoMatch() {
        Flight updatedFlight = sampleFlight("FL-1", 40, 25000.0);
        when(flightService.updateFlight("FL-1", updatedFlight)).thenReturn(Optional.empty());

        ResponseEntity<Flight> response = flightController.updateFlight("FL-1", updatedFlight);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAvailableSeatsReturnsBadRequestWhenMissingValue() {
        ResponseEntity<Flight> response = flightController.updateAvailableSeats(
                "FL-1",
                Map.of()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(flightService, never()).updateAvailableSeats("FL-1", 0);
    }

    @Test
    void updatePriceReturnsBadRequestWhenMissingValue() {
        ResponseEntity<Flight> response = flightController.updatePrice(
                "FL-1",
                Map.of()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(flightService, never()).updatePrice("FL-1", 0.0);
    }

    @Test
    void reduceSeatsRejectsInvalidInternalKey() {
        ResponseEntity<Flight> response = flightController.reduceSeatsAfterConfirmedPayment(
                "FL-1",
                "wrong-key",
                Map.of("seatCount", 2)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(flightService, never()).getFlightById("FL-1");
    }

    @Test
    void reduceSeatsRejectsMissingSeatCount() {
        ResponseEntity<Flight> response = flightController.reduceSeatsAfterConfirmedPayment(
                "FL-1",
                "booking-internal-key",
                Map.of()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reduceSeatsRejectsNonPositiveSeatCount() {
        ResponseEntity<Flight> response = flightController.reduceSeatsAfterConfirmedPayment(
                "FL-1",
                "booking-internal-key",
                Map.of("seatCount", 0)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reduceSeatsReturnsNotFoundWhenFlightMissing() {
        when(flightService.getFlightById("FL-1")).thenReturn(Optional.empty());

        ResponseEntity<Flight> response = flightController.reduceSeatsAfterConfirmedPayment(
                "FL-1",
                "booking-internal-key",
                Map.of("seatCount", 2)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void reduceSeatsReturnsConflictWhenNotEnoughSeatsRemain() {
        Flight existing = sampleFlight("FL-1", 1, 25000.0);
        when(flightService.getFlightById("FL-1")).thenReturn(Optional.of(existing));

        ResponseEntity<Flight> response = flightController.reduceSeatsAfterConfirmedPayment(
                "FL-1",
                "booking-internal-key",
                Map.of("seatCount", 2)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void reduceSeatsUpdatesAvailableSeatsOnSuccess() {
        Flight existing = sampleFlight("FL-1", 10, 25000.0);
        Flight updated = sampleFlight("FL-1", 8, 25000.0);
        when(flightService.getFlightById("FL-1")).thenReturn(Optional.of(existing));
        when(flightService.updateAvailableSeats("FL-1", 8)).thenReturn(Optional.of(updated));

        ResponseEntity<Flight> response = flightController.reduceSeatsAfterConfirmedPayment(
                "FL-1",
                "booking-internal-key",
                Map.of("seatCount", 2)
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(updated);

        ArgumentCaptor<Integer> seatsCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(flightService).updateAvailableSeats(eq("FL-1"), seatsCaptor.capture());
        assertThat(seatsCaptor.getValue()).isEqualTo(8);
    }

    private Flight sampleFlight(String flightId, int seats, double price) {
        Flight flight = new Flight();
        flight.setFlightId(flightId);
        flight.setFlightNumber("AL101");
        flight.setFromLocation("Colombo");
        flight.setToLocation("Dubai");
        flight.setDepartureTime("2026-06-05T08:00:00");
        flight.setArrivalTime("2026-06-05T12:00:00");
        flight.setPrice(price);
        flight.setAvailableSeats(seats);
        flight.setStatus("SCHEDULED");
        return flight;
    }
}