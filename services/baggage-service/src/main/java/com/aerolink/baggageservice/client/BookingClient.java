package com.aerolink.baggageservice.client;

import com.aerolink.baggageservice.dto.BookingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class BookingClient {

    private final RestClient restClient;

    public BookingClient(@Value("${services.booking.base-url}") String bookingServiceBaseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceBaseUrl)
                .build();
    }

    public Optional<BookingResponse> getBookingById(String bookingId) {
        try {
            BookingResponse booking = restClient.get()
                    .uri("/bookings/{bookingId}", bookingId)
                    .retrieve()
                    .body(BookingResponse.class);

            return Optional.ofNullable(booking);

        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Booking Service is unavailable. Ensure it is running on port 8081."
            );
        }
    }
}