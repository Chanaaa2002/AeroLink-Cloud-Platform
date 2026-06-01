package com.aerolink.paymentservice.controller;

import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
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
            /*
             * Step 1: Verify that the event genuinely came from Stripe.
             * No payment update happens unless this succeeds.
             */
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

            /*
             * Step 2: Read the Stripe Checkout Session.
             *
             * getObject() can be empty when the Stripe event API version
             * and the Stripe Java library model version do not safely match.
             *
             * Because the signature is already verified and we only need
             * our own metadata fields, we use deserializeUnsafe() as the
             * fallback for this sandbox integration.
             */
            EventDataObjectDeserializer deserializer =
                    event.getDataObjectDeserializer();

            StripeObject stripeObject = deserializer.getObject()
                    .orElseGet(() -> {
                        try {
                            System.out.println(
                                    "Stripe event required fallback deserialization for Checkout Session."
                            );
                            return deserializer.deserializeUnsafe();
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Stripe Checkout Session could not be deserialized: "
                                            + exception.getMessage(),
                                    exception
                            );
                        }
                    });

            if (!(stripeObject instanceof Session session)) {
                throw new IllegalStateException(
                        "Stripe event does not contain a Checkout Session."
                );
            }

            /*
             * Step 3: Confirm that Stripe considers the payment paid.
             */
            if (!"paid".equals(session.getPaymentStatus())) {
                return ResponseEntity.ok(Map.of(
                        "received", "true",
                        "eventType", event.getType(),
                        "action", "ignored_not_paid"
                ));
            }

            /*
             * Step 4: Read the paymentId that our application stored
             * inside Stripe metadata when checkout was created.
             */
            String paymentId = session.getMetadata().get("paymentId");

            if (paymentId == null || paymentId.isBlank()) {
                throw new IllegalStateException(
                        "Stripe Checkout Session does not contain paymentId metadata."
                );
            }

            /*
             * Step 5: Complete the local system update.
             * This updates payment, confirms booking, and reduces seats.
             */
            Payment completedPayment = paymentService.completeStripePayment(paymentId);

            return ResponseEntity.ok(Map.of(
                    "received", "true",
                    "eventType", event.getType(),
                    "paymentId", completedPayment.getPaymentId(),
                    "paymentStatus", completedPayment.getPaymentStatus()
            ));

        } catch (SignatureVerificationException exception) {
            System.err.println("STRIPE WEBHOOK SIGNATURE FAILED: " + exception.getMessage());
            exception.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                    "received", "false",
                    "message", "Invalid Stripe webhook signature."
            ));

        } catch (Exception exception) {
            String errorMessage = exception.getMessage() != null
                    ? exception.getMessage()
                    : "Unknown webhook processing error.";

            System.err.println("STRIPE WEBHOOK PROCESSING FAILED: " + errorMessage);
            exception.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "received", "false",
                    "message", errorMessage
            ));
        }
    }
}