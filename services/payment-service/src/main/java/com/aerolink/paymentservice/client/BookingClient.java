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
    private final String internalBookingKey;

    public BookingClient(
            @Value("${services.booking.base-url}") String bookingServiceBaseUrl,
            @Value("${services.internal.booking-key}") String internalBookingKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(bookingServiceBaseUrl)
                .build();

        if (internalBookingKey == null || internalBookingKey.isBlank()) {
            throw new IllegalStateException(
                    "PAYMENT_TO_BOOKING_INTERNAL_KEY is not configured."
            );
        }

        this.internalBookingKey = internalBookingKey;
    }

    /**
     * Backend-only booking lookup used before creating a payment.
     * Payment Service sends its private internal key so Booking Service
     * can safely return the real booking amount and payment status.
     */
    public Optional<BookingResponse> getBookingById(String bookingId) {
        try {
            BookingResponse booking = restClient.get()
                    .uri("/bookings/{bookingId}/internal-payment-view", bookingId)
                    .header("X-Internal-Service-Key", internalBookingKey)
                    .retrieve()
                    .body(BookingResponse.class);

            return Optional.ofNullable(booking);

        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();

        } catch (HttpClientErrorException.Forbidden exception) {
            throw new IllegalStateException(
                    "Booking Service rejected the internal Payment Service key."
            );

        } catch (ResourceAccessException exception) {
            throw new IllegalStateException(
                    "Booking Service is unavailable. Ensure it is running on port 8081."
            );
        }
    }

    /**
     * Backend-only booking confirmation used after Stripe webhook verification.
     * Payment Service sends its private internal key so a passenger cannot
     * bypass Stripe and confirm their own booking manually.
     */
    public BookingResponse confirmPaidBooking(String bookingId) {
        try {
            return restClient.put()
                    .uri("/bookings/{bookingId}/payment-success", bookingId)
                    .header("X-Internal-Service-Key", internalBookingKey)
                    .retrieve()
                    .body(BookingResponse.class);

        } catch (HttpClientErrorException.Forbidden exception) {
            throw new IllegalStateException(
                    "Booking Service rejected the internal Payment Service key."
            );

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