
package com.aerolink.flightservice.controller;

import com.aerolink.flightservice.model.Flight;
import com.aerolink.flightservice.service.FlightService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
