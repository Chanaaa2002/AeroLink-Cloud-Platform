package com.aerolink.paymentservice.service;

import com.aerolink.paymentservice.dto.CheckoutSessionResponse;
import com.aerolink.paymentservice.model.Payment;
import com.aerolink.paymentservice.repository.PaymentRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class StripeCheckoutService {

    private final PaymentRepository paymentRepository;
    private final StripeClient stripeClient;
    private final String successUrl;
    private final String cancelUrl;

    public StripeCheckoutService(
            PaymentRepository paymentRepository,
            StripeClient stripeClient,
            @Value("${stripe.success-url}") String successUrl,
            @Value("${stripe.cancel-url}") String cancelUrl
    ) {
        this.paymentRepository = paymentRepository;
        this.stripeClient = stripeClient;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    /*
     * Creates a Stripe Checkout session only for the authenticated
     * passenger who owns the payment record.
     */
    public CheckoutSessionResponse createCheckoutSession(
            String paymentId,
            String authenticatedUserId
    ) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authenticated passenger identity is required."
            );
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found: " + paymentId
                ));

        /*
         * Ownership protection:
         * A passenger cannot open checkout for another passenger's payment.
         *
         * Old payment records created before Cognito ownership security
         * may have a null userId. Those records are intentionally blocked.
         */
        if (payment.getUserId() == null
                || !authenticatedUserId.equals(payment.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to start checkout for this payment."
            );
        }

        if (!"PENDING".equals(payment.getPaymentStatus())) {
            throw new IllegalArgumentException(
                    "Only pending payments can start checkout."
            );
        }

        /*
         * Use the amount stored by Payment Service, not an amount entered
         * by the passenger in the browser or request body.
         */
        long amountInMinorUnits = Math.round(payment.getAmount() * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(payment.getPaymentId())
                .putMetadata("paymentId", payment.getPaymentId())
                .putMetadata("bookingId", payment.getBookingId())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(payment.getCurrency().toLowerCase(Locale.ROOT))
                                                .setUnitAmount(amountInMinorUnits)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("AeroLink Flight Booking Payment")
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        try {
            Session session = stripeClient.v1()
                    .checkout()
                    .sessions()
                    .create(params);

            return new CheckoutSessionResponse(
                    payment.getPaymentId(),
                    session.getId(),
                    session.getUrl()
            );

        } catch (StripeException exception) {
            throw new IllegalStateException(
                    "Stripe checkout session could not be created: "
                            + exception.getMessage()
            );
        }
    }
}
