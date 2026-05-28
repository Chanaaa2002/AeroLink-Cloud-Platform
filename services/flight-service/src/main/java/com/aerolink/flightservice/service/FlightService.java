
package com.aerolink.flightservice.service;

import com.aerolink.flightservice.model.Flight;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FlightService {

    private final List<Flight> flights = new ArrayList<>();

    public FlightService() {
        flights.add(new Flight(
                "FL001",
                "AL101",
                "Colombo",
                "Dubai",
                "2026-06-01T08:00:00",
                "2026-06-01T12:00:00",
                450.00,
                120,
                "SCHEDULED"
        ));

        flights.add(new Flight(
                "FL002",
                "AL202",
                "Colombo",
                "London",
                "2026-06-02T10:00:00",
                "2026-06-02T20:00:00",
                950.00,
                80,
                "SCHEDULED"
        ));
    }

    public List<Flight> getAllFlights() {
        return flights;
    }

    public Optional<Flight> getFlightById(String flightId) {
        return flights.stream()
                .filter(flight -> flight.getFlightId().equalsIgnoreCase(flightId))
                .findFirst();
    }

    public Flight createFlight(Flight flight) {
        flights.add(flight);
        return flight;
    }

    public Optional<Flight> updateFlight(String flightId, Flight updatedFlight) {
        Optional<Flight> existingFlightOptional = getFlightById(flightId);

        if (existingFlightOptional.isEmpty()) {
            return Optional.empty();
        }

        Flight existingFlight = existingFlightOptional.get();

        existingFlight.setFlightNumber(updatedFlight.getFlightNumber());
        existingFlight.setFromLocation(updatedFlight.getFromLocation());
        existingFlight.setToLocation(updatedFlight.getToLocation());
        existingFlight.setDepartureTime(updatedFlight.getDepartureTime());
        existingFlight.setArrivalTime(updatedFlight.getArrivalTime());
        existingFlight.setPrice(updatedFlight.getPrice());
        existingFlight.setAvailableSeats(updatedFlight.getAvailableSeats());
        existingFlight.setStatus(updatedFlight.getStatus());

        return Optional.of(existingFlight);
    }

   public Optional<Flight> updateAvailableSeats(String flightId, int availableSeats) {
        Optional<Flight> existingFlightOptional = getFlightById(flightId);

        if (existingFlightOptional.isEmpty()) {
            return Optional.empty();
        }

        Flight existingFlight = existingFlightOptional.get();
        existingFlight.setAvailableSeats(availableSeats);

        return Optional.of(existingFlight);
    }

    public Optional<Flight> updatePrice(String flightId, double price) {
        Optional<Flight> existingFlightOptional = getFlightById(flightId);

        if (existingFlightOptional.isEmpty()) {
            return Optional.empty();
        }

        Flight existingFlight = existingFlightOptional.get();
        existingFlight.setPrice(price);

        return Optional.of(existingFlight);
    }
}
