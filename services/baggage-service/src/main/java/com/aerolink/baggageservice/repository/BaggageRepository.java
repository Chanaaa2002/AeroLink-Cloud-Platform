package com.aerolink.baggageservice.repository;

import com.aerolink.baggageservice.model.Baggage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class BaggageRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public BaggageRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.baggage-table}") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public Baggage save(Baggage baggage) {
        dynamoDbClient.putItem(
                PutItemRequest.builder()
                        .tableName(tableName)
                        .item(toItem(baggage))
                        .build()
        );

        return baggage;
    }

    public Optional<Baggage> findById(String baggageId) {
        Map<String, AttributeValue> key = Map.of(
                "baggageId", AttributeValue.builder().s(baggageId).build()
        );

        Map<String, AttributeValue> item = dynamoDbClient.getItem(
                GetItemRequest.builder()
                        .tableName(tableName)
                        .key(key)
                        .build()
        ).item();

        if (item == null || item.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(fromItem(item));
    }

    private Map<String, AttributeValue> toItem(Baggage baggage) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("baggageId", AttributeValue.builder().s(baggage.getBaggageId()).build());
        item.put("bookingId", AttributeValue.builder().s(baggage.getBookingId()).build());
        item.put("tagNumber", AttributeValue.builder().s(baggage.getTagNumber()).build());
        item.put("status", AttributeValue.builder().s(baggage.getStatus()).build());
        item.put("currentLocation", AttributeValue.builder().s(baggage.getCurrentLocation()).build());
        item.put("lastUpdated", AttributeValue.builder().s(baggage.getLastUpdated()).build());

        return item;
    }

    private Baggage fromItem(Map<String, AttributeValue> item) {
        return new Baggage(
                item.get("baggageId").s(),
                item.get("bookingId").s(),
                item.get("tagNumber").s(),
                item.get("status").s(),
                item.get("currentLocation").s(),
                item.get("lastUpdated").s()
        );
    }
}