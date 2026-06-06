package com.aerolink.flightservice.service;

import com.aerolink.flightservice.model.Flight;
import com.aerolink.flightservice.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightService flightService;

    @Test
    void getAllFlightsReturnsRepositoryResults() {
        Flight flight = sampleFlight("FL-1", 40, 25000.0);
        when(flightRepository.findAll()).thenReturn(List.of(flight));

        List<Flight> flights = flightService.getAllFlights();

        assertThat(flights).containsExactly(flight);
        verify(flightRepository).findAll();
    }

    @Test
    void getFlightByIdReturnsRepositoryResult() {
        Flight flight = sampleFlight("FL-1", 40, 25000.0);
        when(flightRepository.findById("FL-1")).thenReturn(Optional.of(flight));

        assertThat(flightService.getFlightById("FL-1")).containsSame(flight);
    }

    @Test
    void createFlightSavesFlight() {
        Flight flight = sampleFlight("FL-1", 40, 25000.0);

        when(flightRepository.save(flight)).thenReturn(flight);

        assertThat(flightService.createFlight(flight)).isSameAs(flight);
        verify(flightRepository).save(flight);
    }

    @Test
    void updateFlightReplacesIdAndPersistsUpdatedFlight() {
        Flight existing = sampleFlight("FL-1", 40, 25000.0);
        Flight updated = sampleFlight("WRONG-ID", 60, 27000.0);
        when(flightRepository.findById("FL-1")).thenReturn(Optional.of(existing));
        when(flightRepository.save(updated)).thenReturn(updated);

        Optional<Flight> result = flightService.updateFlight("FL-1", updated);

        assertThat(result).containsSame(updated);
        assertThat(updated.getFlightId()).isEqualTo("FL-1");
        verify(flightRepository).save(updated);
    }

    @Test
    void updateFlightReturnsEmptyWhenMissing() {
        when(flightRepository.findById("FL-1")).thenReturn(Optional.empty());

        assertThat(flightService.updateFlight("FL-1", sampleFlight("WRONG", 60, 27000.0)))
                .isEmpty();
    }

    @Test
    void updateAvailableSeatsPersistsSeatChange() {
        Flight existing = sampleFlight("FL-1", 40, 25000.0);
        when(flightRepository.findById("FL-1")).thenReturn(Optional.of(existing));
        when(flightRepository.save(existing)).thenReturn(existing);

        Optional<Flight> result = flightService.updateAvailableSeats("FL-1", 25);

        assertThat(result).containsSame(existing);
        assertThat(existing.getAvailableSeats()).isEqualTo(25);
        verify(flightRepository).save(existing);
    }

    @Test
    void updatePricePersistsPriceChange() {
        Flight existing = sampleFlight("FL-1", 40, 25000.0);
        when(flightRepository.findById("FL-1")).thenReturn(Optional.of(existing));
        when(flightRepository.save(existing)).thenReturn(existing);

        Optional<Flight> result = flightService.updatePrice("FL-1", 30000.0);

        assertThat(result).containsSame(existing);
        assertThat(existing.getPrice()).isEqualTo(30000.0);
        verify(flightRepository).save(existing);
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