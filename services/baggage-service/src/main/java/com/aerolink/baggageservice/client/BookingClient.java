package com.aerolink.baggageservice.client;

import com.aerolink.baggageservice.dto.BookingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Component
public class BookingClient {

    private final RestClient restClient;

    public BookingClient(@Value("${services.booking.base-url}") String bookingServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceBaseUrl)
                .build();
    }

    /*
     * Secure booking lookup used for baggage operations.
     *
     * For staff baggage creation, the verified STAFF token is forwarded
     * so Booking Service can validate the confirmed and paid booking.
     *
     * For passenger tracking by booking ID, the verified PASSENGER token
     * is forwarded so Booking Service can confirm ownership of the booking.
     */
    public Optional<BookingResponse> getBookingById(
            String bookingId,
            String accessToken
    ) {
        try {
            BookingResponse booking = restClient.get()
                    .uri("/bookings/{bookingId}", bookingId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(BookingResponse.class);

            return Optional.ofNullable(booking);

        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();

        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication was rejected while validating booking access."
            );

        } catch (HttpClientErrorException.Forbidden exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to access baggage for this booking."
            );

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Booking Service is unavailable. Ensure it is running on port 8081."
            );
        }
    }
}