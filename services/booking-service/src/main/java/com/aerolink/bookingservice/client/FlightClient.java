package com.aerolink.bookingservice.client;

import com.aerolink.bookingservice.dto.FlightResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import org.springframework.http.MediaType;
import java.util.Map;

@Component
public class FlightClient {

    private final RestClient restClient;

    public FlightClient(@Value("${services.flight.base-url}") String flightServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(flightServiceBaseUrl)
                .build();
    }

    public Optional<FlightResponse> getFlightById(String flightId) {
        try {
            FlightResponse flight = restClient.get()
                    .uri("/flights/{flightId}", flightId)
                    .retrieve()
                    .body(FlightResponse.class);

            return Optional.ofNullable(flight);

        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Flight Service is unavailable. Ensure it is running on port 8080."
            );
        }
    }

    public FlightResponse updateAvailableSeats(String flightId, int availableSeats) {
        try {
            return restClient.put()
                    .uri("/flights/{flightId}/seats", flightId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("availableSeats", availableSeats))
                    .retrieve()
                    .body(FlightResponse.class);

        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalArgumentException("Flight not found: " + flightId);

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Flight Service is unavailable. Ensure it is running on port 8080."
            );
        }
    }
}