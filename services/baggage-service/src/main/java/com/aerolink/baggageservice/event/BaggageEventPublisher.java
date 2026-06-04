package com.aerolink.baggageservice.event;

import com.aerolink.baggageservice.model.Baggage;
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
public class BaggageEventPublisher {

    private static final Logger logger =
            LoggerFactory.getLogger(BaggageEventPublisher.class);

    private final EventBridgeClient eventBridgeClient;
    private final JsonMapper jsonMapper;
    private final String eventBusName;

    public BaggageEventPublisher(
            @Value("${aws.eventbridge.bus-name}") String eventBusName,
            @Value("${aws.eventbridge.region}") String eventBridgeRegion
    ) {
        this.eventBusName = eventBusName;
        this.eventBridgeClient = EventBridgeClient.builder()
                .region(Region.of(eventBridgeRegion))
                .build();
        this.jsonMapper = JsonMapper.builder().build();
    }

    public void publishStatusUpdated(Baggage baggage) {
        /*
         * Do not publish notifications for legacy/unowned baggage records.
         * Passenger notifications must always be linked to a trusted owner.
         */
        if (baggage.getUserId() == null || baggage.getUserId().isBlank()) {
            logger.warn(
                    "Skipped BaggageStatusUpdated event because no trusted passenger owner exists."
            );
            return;
        }

        try {
            Map<String, String> detail = new LinkedHashMap<>();
            detail.put("userId", baggage.getUserId());
            detail.put("bookingId", baggage.getBookingId());
            detail.put("baggageId", baggage.getBaggageId());
            detail.put("status", baggage.getStatus());
            detail.put("currentLocation", baggage.getCurrentLocation());

            String detailJson = jsonMapper.writeValueAsString(detail);

            PutEventsRequestEntry eventEntry = PutEventsRequestEntry.builder()
                    .eventBusName(eventBusName)
                    .source("aerolink.baggage-service")
                    .detailType("BaggageStatusUpdated")
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
                        "BaggageStatusUpdated event was rejected by EventBridge."
                );
                return;
            }

            logger.info(
                    "BaggageStatusUpdated event published successfully."
            );

        } catch (Exception exception) {
            /*
             * Notification publishing must not cancel an already successful
             * baggage operational update. The failure is logged for monitoring.
             */
            logger.error(
                    "Unable to publish BaggageStatusUpdated event.",
                    exception
            );
        }
    }
}