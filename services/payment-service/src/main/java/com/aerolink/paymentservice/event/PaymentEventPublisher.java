package com.aerolink.paymentservice.event;

import com.aerolink.paymentservice.model.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PaymentEventPublisher {

    private static final Logger logger =
            LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final EventBridgeClient eventBridgeClient;
    private final JsonMapper jsonMapper;
    private final String eventBusName;

    public PaymentEventPublisher(
            @Value("${aws.eventbridge.bus-name}") String eventBusName,
            @Value("${aws.eventbridge.region}") String eventBridgeRegion
    ) {
        this.eventBusName = eventBusName;
        this.eventBridgeClient = EventBridgeClient.builder()
                .region(Region.of(eventBridgeRegion))
                .build();
        this.jsonMapper = JsonMapper.builder().build();
    }

    public void publishPaymentSucceeded(Payment payment) {
        /*
         * Passenger notifications must only be created for payments
         * that are linked to a trusted authenticated Cognito owner.
         */
        if (payment.getUserId() == null || payment.getUserId().isBlank()) {
            logger.warn(
                    "Skipped PaymentSucceeded event because no trusted passenger owner exists."
            );
            return;
        }

        try {
            Map<String, String> detail = new LinkedHashMap<>();
            detail.put("userId", payment.getUserId());
            detail.put("bookingId", payment.getBookingId());
            detail.put("paymentId", payment.getPaymentId());

            String detailJson = jsonMapper.writeValueAsString(detail);

            PutEventsRequestEntry eventEntry = PutEventsRequestEntry.builder()
                    .eventBusName(eventBusName)
                    .source("aerolink.payment-service")
                    .detailType("PaymentSucceeded")
                    .detail(detailJson)
                    .build();

            PutEventsResponse response = eventBridgeClient.putEvents(
                    PutEventsRequest.builder()
                            .entries(eventEntry)
                            .build()
            );

            if (response.failedEntryCount() != null
                    && response.failedEntryCount() > 0) {
                logger.error(
                        "PaymentSucceeded event was rejected by EventBridge."
                );
                return;
            }

            logger.info("PaymentSucceeded event published successfully.");

        } catch (Exception exception) {
            /*
             * Notification publishing must not undo an already completed
             * Stripe payment and confirmed booking.
             */
            logger.error(
                    "Unable to publish PaymentSucceeded event.",
                    exception
            );
        }
    }
}