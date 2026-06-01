package com.aerolink.bookingservice.client;

import com.aerolink.bookingservice.dto.FlightResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

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
}