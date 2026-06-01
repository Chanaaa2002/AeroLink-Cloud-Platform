package com.aerolink.baggageservice.repository;

import com.aerolink.baggageservice.model.Baggage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class BaggageRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String bookingIndexName;

    public BaggageRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.baggage-table}") String tableName,
            @Value("${aws.dynamodb.baggage-booking-index}") String bookingIndexName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.bookingIndexName = bookingIndexName;
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

    public List<Baggage> findByBookingId(String bookingId) {
        Map<String, String> attributeNames = Map.of(
                "#bookingId", "bookingId"
        );

        Map<String, AttributeValue> attributeValues = Map.of(
                ":bookingId", AttributeValue.builder().s(bookingId).build()
        );

        List<Map<String, AttributeValue>> items = dynamoDbClient.query(
                QueryRequest.builder()
                        .tableName(tableName)
                        .indexName(bookingIndexName)
                        .keyConditionExpression("#bookingId = :bookingId")
                        .expressionAttributeNames(attributeNames)
                        .expressionAttributeValues(attributeValues)
                        .build()
        ).items();

        List<Baggage> baggageList = new ArrayList<>();

        for (Map<String, AttributeValue> item : items) {
            baggageList.add(fromItem(item));
        }

        return baggageList;
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