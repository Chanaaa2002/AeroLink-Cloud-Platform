package com.aerolink.flightservice.service;

import com.aerolink.flightservice.model.Flight;
import com.aerolink.flightservice.repository.FlightRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FlightService {

    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    public List<Flight> getAllFlights() {
        return flightRepository.findAll();
    }

    public Optional<Flight> getFlightById(String flightId) {
        return flightRepository.findById(flightId);
    }

    public Flight createFlight(Flight flight) {
        return flightRepository.save(flight);
    }

    public Optional<Flight> updateFlight(String flightId, Flight updatedFlight) {
        if (flightRepository.findById(flightId).isEmpty()) {
            return Optional.empty();
        }

        updatedFlight.setFlightId(flightId);
        return Optional.of(flightRepository.save(updatedFlight));
    }

    public Optional<Flight> updateAvailableSeats(String flightId, int availableSeats) {
        Optional<Flight> existingFlightOptional = flightRepository.findById(flightId);

        if (existingFlightOptional.isEmpty()) {
            return Optional.empty();
        }

        Flight existingFlight = existingFlightOptional.get();
        existingFlight.setAvailableSeats(availableSeats);

        return Optional.of(flightRepository.save(existingFlight));
    }

    public Optional<Flight> updatePrice(String flightId, double price) {
        Optional<Flight> existingFlightOptional = flightRepository.findById(flightId);

        if (existingFlightOptional.isEmpty()) {
            return Optional.empty();
        }

        Flight existingFlight = existingFlightOptional.get();
        existingFlight.setPrice(price);

        return Optional.of(flightRepository.save(existingFlight));
    }
}