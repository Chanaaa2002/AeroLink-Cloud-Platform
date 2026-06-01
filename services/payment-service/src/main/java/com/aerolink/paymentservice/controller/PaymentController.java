package com.aerolink.paymentservice.controller;

import com.aerolink.paymentservice.dto.CheckoutSessionResponse;
import com.aerolink.paymentservice.dto.CreatePaymentRequest;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.service.PaymentService;
import com.aerolink.paymentservice.service.StripeCheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody CreatePaymentRequest request) {
        Payment createdPayment = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    @PostMapping("/{paymentId}/checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @PathVariable String paymentId
    ) {
        CheckoutSessionResponse response =
                stripeCheckoutService.createCheckoutSession(paymentId);

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