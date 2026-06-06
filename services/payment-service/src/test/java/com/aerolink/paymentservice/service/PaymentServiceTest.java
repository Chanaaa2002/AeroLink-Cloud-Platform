package com.aerolink.paymentservice.service;

import com.aerolink.paymentservice.client.BookingClient;
import com.aerolink.paymentservice.dto.BookingResponse;
import com.aerolink.paymentservice.dto.CreatePaymentRequest;
import com.aerolink.paymentservice.event.PaymentEventPublisher;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void getAllPaymentsReturnsRepositoryResults() {
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "PENDING");
        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        assertThat(paymentService.getAllPayments()).containsExactly(payment);
    }

    @Test
    void createPaymentRejectsBlankAuthenticatedUser() {
        CreatePaymentRequest request = createRequest("BK-1");

        assertThatThrownBy(() -> paymentService.createPayment(request, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Authenticated passenger identity is required.");
    }

    @Test
    void createPaymentRejectsMissingBooking() {
        CreatePaymentRequest request = createRequest("BK-1");
        when(bookingClient.getBookingById("BK-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(request, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking not found: BK-1");
    }

    @Test
    void createPaymentRejectsOwnershipMismatch() {
        CreatePaymentRequest request = createRequest("BK-1");
        BookingResponse booking = bookingResponse("BK-1", "user-2", "PENDING_PAYMENT", "PENDING", 50000.0);
        when(bookingClient.getBookingById("BK-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createPayment(request, "user-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You are not allowed to create a payment for this booking.");
    }

    @Test
    void createPaymentRejectsNonPendingBookingStatus() {
        CreatePaymentRequest request = createRequest("BK-1");
        BookingResponse booking = bookingResponse("BK-1", "user-1", "CONFIRMED", "PENDING", 50000.0);
        when(bookingClient.getBookingById("BK-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createPayment(request, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Booking is not waiting for payment.");
    }

    @Test
    void createPaymentRejectsNonPendingPaymentStatus() {
        CreatePaymentRequest request = createRequest("BK-1");
        BookingResponse booking = bookingResponse("BK-1", "user-1", "PENDING_PAYMENT", "PAID", 50000.0);
        when(bookingClient.getBookingById("BK-1")).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.createPayment(request, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment is not pending for this booking.");
    }

    @Test
    void createPaymentPersistsPendingPayment() {
        CreatePaymentRequest request = createRequest("BK-1");
        BookingResponse booking = bookingResponse("BK-1", "user-1", "PENDING_PAYMENT", "PENDING", 50000.0);
        when(bookingClient.getBookingById("BK-1")).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment payment = paymentService.createPayment(request, "user-1");

        assertThat(payment.getBookingId()).isEqualTo("BK-1");
        assertThat(payment.getUserId()).isEqualTo("user-1");
        assertThat(payment.getAmount()).isEqualTo(50000.0);
        assertThat(payment.getCurrency()).isEqualTo("LKR");
        assertThat(payment.getPaymentMethod()).isEqualTo("STRIPE_CHECKOUT_TEST");
        assertThat(payment.getPaymentStatus()).isEqualTo("PENDING");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void completeStripePaymentPublishesEventAfterSuccessfulPersist() {
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "PENDING");
        when(paymentRepository.findById("PAY-1")).thenReturn(Optional.of(payment));
        when(bookingClient.confirmPaidBooking("BK-1")).thenReturn(bookingResponse("BK-1", "user-1", "CONFIRMED", "PAID", 50000.0));
        when(paymentRepository.save(payment)).thenAnswer(invocation -> invocation.getArgument(0));

        Payment completed = paymentService.completeStripePayment("PAY-1");

        assertThat(completed.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(completed.getProcessedAt()).isNotBlank();

        InOrder order = inOrder(paymentRepository, paymentEventPublisher);
        order.verify(paymentRepository).save(payment);
        order.verify(paymentEventPublisher).publishPaymentSucceeded(payment);
    }

    @Test
    void completeStripePaymentIsIdempotentForAlreadySuccessfulPayment() {
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "SUCCESS");
        when(paymentRepository.findById("PAY-1")).thenReturn(Optional.of(payment));

        Payment completed = paymentService.completeStripePayment("PAY-1");

        assertThat(completed).isSameAs(payment);
        verify(bookingClient, never()).confirmPaidBooking(any());
        verify(paymentRepository, never()).save(any());
        verify(paymentEventPublisher, never()).publishPaymentSucceeded(any());
    }

    @Test
    void completeStripePaymentRejectsNonPendingPayment() {
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "FAILED");
        when(paymentRepository.findById("PAY-1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.completeStripePayment("PAY-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment is not pending and cannot be completed.");
    }

    @Test
    void completeStripePaymentRejectsMissingPayment() {
        when(paymentRepository.findById("PAY-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.completeStripePayment("PAY-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Payment not found: PAY-1");
    }

    private CreatePaymentRequest createRequest(String bookingId) {
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId(bookingId);
        return request;
    }

    private BookingResponse bookingResponse(
            String bookingId,
            String userId,
            String bookingStatus,
            String paymentStatus,
            double totalAmount
    ) {
        BookingResponse booking = new BookingResponse();
        booking.setBookingId(bookingId);
        booking.setUserId(userId);
        booking.setBookingStatus(bookingStatus);
        booking.setPaymentStatus(paymentStatus);
        booking.setTotalAmount(totalAmount);
        return booking;
    }

    private Payment samplePayment(String paymentId, String bookingId, String userId, String paymentStatus) {
        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(50000.0);
        payment.setCurrency("LKR");
        payment.setPaymentMethod("STRIPE_CHECKOUT_TEST");
        payment.setPaymentStatus(paymentStatus);
        payment.setCreatedAt("2026-06-05T08:00:00");
        payment.setProcessedAt(null);
        return payment;
    }
}