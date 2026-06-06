package com.aerolink.paymentservice.controller;

import com.aerolink.paymentservice.dto.CheckoutSessionResponse;
import com.aerolink.paymentservice.dto.CreatePaymentRequest;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.service.PaymentService;
import com.aerolink.paymentservice.service.StripeCheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    private final PaymentService paymentService = mock(PaymentService.class);
    private final StripeCheckoutService stripeCheckoutService = mock(StripeCheckoutService.class);
    private final PaymentController paymentController = new PaymentController(
            paymentService,
            stripeCheckoutService
    );

    @Test
    void getAllPaymentsReturnsPayments() {
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "PENDING");
        when(paymentService.getAllPayments()).thenReturn(List.of(payment));

        assertThat(paymentController.getAllPayments()).containsExactly(payment);
    }

    @Test
    void getPaymentByIdReturnsOkForOwner() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"));
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "PENDING");
        when(paymentService.getPaymentById("PAY-1")).thenReturn(Optional.of(payment));

        ResponseEntity<Payment> response = paymentController.getPaymentById("PAY-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(payment);
    }

    @Test
    void getPaymentByIdReturnsOkForStaff() {
        Jwt jwt = jwt("user-2", List.of("STAFF"));
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "PENDING");
        when(paymentService.getPaymentById("PAY-1")).thenReturn(Optional.of(payment));

        ResponseEntity<Payment> response = paymentController.getPaymentById("PAY-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getPaymentByIdReturnsForbiddenForOtherPassenger() {
        Jwt jwt = jwt("user-2", List.of("PASSENGER"));
        Payment payment = samplePayment("PAY-1", "BK-1", "user-1", "PENDING");
        when(paymentService.getPaymentById("PAY-1")).thenReturn(Optional.of(payment));

        ResponseEntity<Payment> response = paymentController.getPaymentById("PAY-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void getPaymentByIdReturnsNotFoundWhenMissing() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"));
        when(paymentService.getPaymentById("PAY-1")).thenReturn(Optional.empty());

        ResponseEntity<Payment> response = paymentController.getPaymentById("PAY-1", jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createPaymentUsesJwtSubject() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"));
        CreatePaymentRequest request = new CreatePaymentRequest();
        request.setBookingId("BK-1");

        Payment created = samplePayment("PAY-1", "BK-1", "user-1", "PENDING");
        when(paymentService.createPayment(request, "user-1")).thenReturn(created);

        ResponseEntity<Payment> response = paymentController.createPayment(request, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(created);
        verify(paymentService).createPayment(request, "user-1");
    }

    @Test
    void createCheckoutSessionDelegatesToStripeCheckoutService() {
        Jwt jwt = jwt("user-1", List.of("PASSENGER"));
        CheckoutSessionResponse checkoutSessionResponse = new CheckoutSessionResponse(
                "PAY-1",
                "cs_test_123",
                "https://stripe.test/checkout"
        );
        when(stripeCheckoutService.createCheckoutSession("PAY-1", "user-1"))
                .thenReturn(checkoutSessionResponse);

        ResponseEntity<CheckoutSessionResponse> response = paymentController.createCheckoutSession(
                "PAY-1",
                jwt
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(checkoutSessionResponse);
    }

    @Test
    void checkoutSuccessWithoutSessionIdReturnsWaitingMessage() {
        ResponseEntity<Map<String, String>> response = paymentController.checkoutSuccess(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("paymentConfirmation", "Waiting for secure webhook confirmation.");
    }

    @Test
    void checkoutCancelledReturnsPendedStatus() {
        ResponseEntity<Map<String, String>> response = paymentController.checkoutCancelled();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("paymentStatus", "PENDING");
    }

    private Jwt jwt(String subject, List<String> groups) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(subject);
        when(jwt.getClaimAsStringList("cognito:groups")).thenReturn(groups);
        return jwt;
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