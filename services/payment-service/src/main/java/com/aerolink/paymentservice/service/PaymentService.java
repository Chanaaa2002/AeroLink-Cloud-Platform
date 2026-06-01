package com.aerolink.paymentservice.service;

import com.aerolink.paymentservice.client.BookingClient;
import com.aerolink.paymentservice.dto.BookingResponse;
import com.aerolink.paymentservice.dto.CreatePaymentRequest;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingClient bookingClient;

    public PaymentService(PaymentRepository paymentRepository, BookingClient bookingClient) {
        this.paymentRepository = paymentRepository;
        this.bookingClient = bookingClient;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> getPaymentById(String paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Payment createPayment(CreatePaymentRequest request) {
        if (request.getBookingId() == null || request.getBookingId().isBlank()) {
            throw new IllegalArgumentException("Booking ID is required.");
        }

        BookingResponse booking = bookingClient.getBookingById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + request.getBookingId()
                ));

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
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency("LKR");
        payment.setPaymentMethod("DUMMY");
        payment.setPaymentStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now().toString());
        payment.setProcessedAt(null);

        return paymentRepository.save(payment);
    }
}