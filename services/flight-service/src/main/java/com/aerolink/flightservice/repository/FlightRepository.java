package com.aerolink.flightservice.repository;

import com.aerolink.flightservice.model.Flight;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class FlightRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public FlightRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.flights-table}") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public Flight save(Flight flight) {
        dynamoDbClient.putItem(
                PutItemRequest.builder()
                        .tableName(tableName)
                        .item(toItem(flight))
                        .build()
        );

        return flight;
    }

    public List<Flight> findAll() {
        return dynamoDbClient.scan(
                        ScanRequest.builder()
                                .tableName(tableName)
                                .build()
                ).items()
                .stream()
                .map(this::fromItem)
                .toList();
    }

    public Optional<Flight> findById(String flightId) {
        Map<String, AttributeValue> key = Map.of(
                "flightId", AttributeValue.builder().s(flightId).build()
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

    private Map<String, AttributeValue> toItem(Flight flight) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("flightId", AttributeValue.builder().s(flight.getFlightId()).build());
        item.put("flightNumber", AttributeValue.builder().s(flight.getFlightNumber()).build());
        item.put("fromLocation", AttributeValue.builder().s(flight.getFromLocation()).build());
        item.put("toLocation", AttributeValue.builder().s(flight.getToLocation()).build());
        item.put("departureTime", AttributeValue.builder().s(flight.getDepartureTime()).build());
        item.put("arrivalTime", AttributeValue.builder().s(flight.getArrivalTime()).build());
        item.put("price", AttributeValue.builder().n(String.valueOf(flight.getPrice())).build());
        item.put("availableSeats", AttributeValue.builder().n(String.valueOf(flight.getAvailableSeats())).build());
        item.put("status", AttributeValue.builder().s(flight.getStatus()).build());

        return item;
    }

    private Flight fromItem(Map<String, AttributeValue> item) {
        return new Flight(
                item.get("flightId").s(),
                item.get("flightNumber").s(),
                item.get("fromLocation").s(),
                item.get("toLocation").s(),
                item.get("departureTime").s(),
                item.get("arrivalTime").s(),
                Double.parseDouble(item.get("price").n()),
                Integer.parseInt(item.get("availableSeats").n()),
                item.get("status").s()
        );
    }
}