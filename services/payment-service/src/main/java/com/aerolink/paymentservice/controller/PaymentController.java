package com.aerolink.paymentservice.controller;

import com.aerolink.paymentservice.dto.CheckoutSessionResponse;
import com.aerolink.paymentservice.dto.CreatePaymentRequest;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.service.PaymentService;
import com.aerolink.paymentservice.service.StripeCheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final StripeCheckoutService stripeCheckoutService;

    public PaymentController(
            PaymentService paymentService,
            StripeCheckoutService stripeCheckoutService
    ) {
        this.paymentService = paymentService;
        this.stripeCheckoutService = stripeCheckoutService;
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentService.getAllPayments();
    }

   @GetMapping("/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return paymentService.getPaymentById(paymentId)
                .map(payment -> {
                    List<String> groups = jwt.getClaimAsStringList("cognito:groups");

                    boolean isStaff = groups != null && groups.contains("STAFF");
                    boolean isPaymentOwner = payment.getUserId() != null
                            && jwt.getSubject().equals(payment.getUserId());

                    if (isStaff || isPaymentOwner) {
                        return ResponseEntity.ok(payment);
                    }

                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .<Payment>build();
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        /*
         * The passenger identity comes from the verified Cognito access token.
         * Payment Service checks that the booking belongs to this passenger.
         */
        Payment createdPayment = paymentService.createPayment(
                request,
                jwt.getSubject()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    @PostMapping("/{paymentId}/checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @PathVariable String paymentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        /*
         * Stripe Checkout can be created only for a payment that belongs
         * to the authenticated Cognito passenger.
         */
        CheckoutSessionResponse response =
                stripeCheckoutService.createCheckoutSession(
                        paymentId,
                        jwt.getSubject()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/checkout/success")
    public ResponseEntity<Map<String, String>> checkoutSuccess(
            @RequestParam(value = "session_id", required = false) String sessionId
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Stripe checkout returned successfully.",
                    "paymentConfirmation", "Waiting for secure webhook confirmation.",
                    "note", "No Stripe session ID was included in the redirect URL."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Stripe checkout returned successfully.",
                "stripeSessionId", sessionId,
                "paymentConfirmation", "Payment status is confirmed through the secure webhook."
        ));
    }

    @GetMapping("/checkout/cancel")
    public ResponseEntity<Map<String, String>> checkoutCancelled() {
        return ResponseEntity.ok(Map.of(
                "message", "Stripe test checkout was cancelled.",
                "paymentStatus", "PENDING"
        ));
    }
}
