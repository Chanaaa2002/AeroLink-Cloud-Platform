package com.aerolink.paymentservice.controller;

import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments")
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final String webhookSecret;

    public StripeWebhookController(
            PaymentService paymentService,
            @Value("${stripe.webhook-secret}") String webhookSecret
    ) {
        this.paymentService = paymentService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String stripeSignature
    ) {
        try {
            Event event = Webhook.constructEvent(
                    payload,
                    stripeSignature,
                    webhookSecret
            );

            if (!"checkout.session.completed".equals(event.getType())) {
                return ResponseEntity.ok(Map.of(
                        "received", "true",
                        "eventType", event.getType(),
                        "action", "ignored"
                ));
            }

            StripeObject stripeObject = event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new IllegalStateException(
                            "Stripe Checkout Session could not be read."
                    ));

            if (!(stripeObject instanceof Session session)) {
                throw new IllegalStateException(
                        "Stripe event does not contain a Checkout Session."
                );
            }

            String paymentId = session.getMetadata().get("paymentId");

            if (paymentId == null || paymentId.isBlank()) {
                throw new IllegalStateException(
                        "Stripe Checkout Session does not contain paymentId metadata."
                );
            }

            Payment completedPayment = paymentService.completeStripePayment(paymentId);

            return ResponseEntity.ok(Map.of(
                    "received", "true",
                    "eventType", event.getType(),
                    "paymentId", completedPayment.getPaymentId(),
                    "paymentStatus", completedPayment.getPaymentStatus()
            ));

        } catch (SignatureVerificationException exception) {
            return ResponseEntity.badRequest().body(Map.of(
                    "received", "false",
                    "message", "Invalid Stripe webhook signature."
            ));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "received", "false",
                    "message", exception.getMessage()
            ));
        }
    }
}