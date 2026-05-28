
package com.aerolink.flightservice.controller;

import com.aerolink.flightservice.model.Flight;
import com.aerolink.flightservice.service.FlightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @GetMapping
    public List<Flight> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{flightId}")
    public ResponseEntity<Flight> getFlightById(@PathVariable String flightId) {
        return flightService.getFlightById(flightId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Flight createFlight(@RequestBody Flight flight) {
        return flightService.createFlight(flight);
    }

    @PutMapping("/{flightId}")
    public ResponseEntity<Flight> updateFlight(
            @PathVariable String flightId,
            @RequestBody Flight updatedFlight
    ) {
        return flightService.updateFlight(flightId, updatedFlight)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PutMapping("/{flightId}/seats")
    public ResponseEntity<Flight> updateAvailableSeats(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> request
    ) {
        Object seatsValue = request.get("availableSeats");

        if (seatsValue == null) {
            return ResponseEntity.badRequest().build();
        }

        int availableSeats = ((Number) seatsValue).intValue();

        return flightService.updateAvailableSeats(flightId, availableSeats)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{flightId}/price")
    public ResponseEntity<Flight> updatePrice(
            @PathVariable String flightId,
            @RequestBody Map<String, Object> request
    ) {
        Object priceValue = request.get("price");

        if (priceValue == null) {
            return ResponseEntity.badRequest().build();
        }

        double price = ((Number) priceValue).doubleValue();

        return flightService.updatePrice(flightId, price)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
