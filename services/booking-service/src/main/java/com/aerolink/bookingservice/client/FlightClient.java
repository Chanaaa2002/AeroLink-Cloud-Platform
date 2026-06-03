package com.aerolink.bookingservice.client;

import com.aerolink.bookingservice.dto.FlightResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Component
public class FlightClient {

    private final RestClient restClient;
    private final String internalFlightKey;

    public FlightClient(
            @Value("${services.flight.base-url}") String flightServiceBaseUrl,
            @Value("${services.internal.flight-key}") String internalFlightKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(flightServiceBaseUrl)
                .build();

        if (internalFlightKey == null || internalFlightKey.isBlank()) {
            throw new IllegalStateException(
                    "BOOKING_TO_FLIGHT_INTERNAL_KEY is not configured."
            );
        }

        this.internalFlightKey = internalFlightKey;
    }

    /*
     * Existing method retained temporarily.
     * We will remove its use from payment confirmation in the next step.
     */
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

    /*
     * Used when an authenticated passenger creates a booking.
     * The passenger token is forwarded only for the read-only flight check.
     */
    public Optional<FlightResponse> getFlightById(String flightId, String accessToken) {
        try {
            FlightResponse flight = restClient.get()
                    .uri("/flights/{flightId}", flightId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(FlightResponse.class);

            return Optional.ofNullable(flight);

        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();

        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new IllegalStateException(
                    "Flight Service rejected the passenger authentication token."
            );

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Flight Service is unavailable. Ensure it is running on port 8080."
            );
        }
    }

    /*
     * Existing STAFF-style seat update method retained temporarily.
     * Payment confirmation will no longer use this method after the next change.
     */
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

    /*
     * Backend-only operation used after verified Stripe payment confirmation.
     * Booking Service sends its private internal key to Flight Service.
     */
    public FlightResponse reduceSeatsAfterConfirmedPayment(String flightId, int seatCount) {
        try {
            return restClient.put()
                    .uri("/flights/{flightId}/internal-seat-reduction", flightId)
                    .header("X-Internal-Service-Key", internalFlightKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("seatCount", seatCount))
                    .retrieve()
                    .body(FlightResponse.class);

        } catch (HttpClientErrorException.NotFound exception) {
            throw new IllegalArgumentException("Flight not found: " + flightId);

        } catch (HttpClientErrorException.Forbidden exception) {
            throw new IllegalStateException(
                    "Flight Service rejected the internal Booking Service key."
            );

        } catch (HttpClientErrorException.Conflict exception) {
            throw new IllegalArgumentException(
                    "Not enough seats available to confirm this booking."
            );

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Flight Service is unavailable. Ensure it is running on port 8080."
            );
        }
    }
}
