package com.aerolink.paymentservice.client;

import com.aerolink.paymentservice.dto.BookingResponse;
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

    /**
     * Reads an existing booking from Booking Service.
     * Used when creating a payment so Payment Service gets the real booking amount.
     */
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

    /**
     * Confirms a booking after Stripe payment succeeds.
     * Booking Service will mark the booking as paid/confirmed
     * and reduce the seats through Flight Service.
     */
    public BookingResponse confirmPaidBooking(String bookingId) {
        try {
            return restClient.put()
                    .uri("/bookings/{bookingId}/payment-success", bookingId)
                    .retrieve()
                    .body(BookingResponse.class);

        } catch (HttpClientErrorException exception) {
            throw new IllegalStateException(
                    "Booking confirmation failed: " + exception.getResponseBodyAsString()
            );

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Booking Service is unavailable. Ensure it is running on port 8081."
            );
        }
    }
}