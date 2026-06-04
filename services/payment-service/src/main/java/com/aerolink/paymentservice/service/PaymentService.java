package com.aerolink.paymentservice.service;

import com.aerolink.paymentservice.client.BookingClient;
import com.aerolink.paymentservice.dto.BookingResponse;
import com.aerolink.paymentservice.dto.CreatePaymentRequest;
import com.aerolink.paymentservice.event.PaymentEventPublisher;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingClient bookingClient,
            PaymentEventPublisher paymentEventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.bookingClient = bookingClient;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId);
    }

    /*
     * Creates a payment only for the authenticated passenger's own booking.
     * authenticatedUserId comes from the verified Cognito JWT token.
     */
    public Payment createPayment(
            CreatePaymentRequest request,
            String authenticatedUserId
    ) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated passenger identity is required."
            );
        }

        if (request.getBookingId() == null || request.getBookingId().isBlank()) {
            throw new IllegalArgumentException("Booking ID is required.");
        }

        /*
         * Payment Service reads the booking using the backend-only secure route.
         * Booking Service returns the trusted Cognito-linked booking owner.
         */
        BookingResponse booking = bookingClient.getBookingById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + request.getBookingId()
                ));

        /*
         * Ownership protection:
         * A passenger cannot create a payment for another passenger's booking.
         */
        if (booking.getUserId() == null
                || !authenticatedUserId.equals(booking.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to create a payment for this booking."
            );
        }

        if (!"PENDING_PAYMENT".equals(booking.getBookingStatus())) {
            throw new IllegalArgumentException(
                    "Booking is not waiting for payment."
            );
        }

        if (!"PENDING".equals(booking.getPaymentStatus())) {
            throw new IllegalArgumentException(
                    "Payment is not pending for this booking."
            );
        }

        Payment payment = new Payment();
        payment.setPaymentId("PAY-" + UUID.randomUUID());
        payment.setBookingId(booking.getBookingId());
        payment.setUserId(authenticatedUserId);
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency("LKR");
        payment.setPaymentMethod("STRIPE_CHECKOUT_TEST");
        payment.setPaymentStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now().toString());
        payment.setProcessedAt(null);

        return paymentRepository.save(payment);
    }

    /*
     * Called only after Stripe webhook verification confirms a paid checkout.
     *
     * After payment and booking status are completed successfully, the service
     * publishes PaymentSucceeded so the notification Lambda can create an alert.
     */
    public Payment completeStripePayment(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + paymentId
                ));

        /*
         * Stripe can deliver the same webhook more than once.
         * Returning the already completed payment prevents duplicate booking
         * confirmation and duplicate PaymentSucceeded notifications.
         */
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            return payment;
        }

        if (!"PENDING".equals(payment.getPaymentStatus())) {
            throw new IllegalArgumentException(
                    "Payment is not pending and cannot be completed."
            );
        }

        /*
         * Trusted backend flow:
         * Payment Service sends its private internal key to Booking Service.
         * Booking Service confirms the booking and securely reduces seats.
         */
        bookingClient.confirmPaidBooking(payment.getBookingId());

        payment.setPaymentStatus("SUCCESS");
        payment.setProcessedAt(LocalDateTime.now().toString());

        Payment completedPayment = paymentRepository.save(payment);

        /*
         * Publish only after the successful payment state has been persisted.
         */
        paymentEventPublisher.publishPaymentSucceeded(completedPayment);

        return completedPayment;
    }
}