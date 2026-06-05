package com.aerolink.bookingservice.repository;

import com.aerolink.bookingservice.model.Booking;
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
public class BookingRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public BookingRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.bookings-table}") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public Booking save(Booking booking) {
        dynamoDbClient.putItem(
                PutItemRequest.builder()
                        .tableName(tableName)
                        .item(toItem(booking))
                        .build()
        );
        return booking;
    }

    public List<Booking> findAll() {
        return dynamoDbClient.scan(
                        ScanRequest.builder()
                                .tableName(tableName)
                                .build()
                ).items()
                .stream()
                .map(this::fromItem)
                .toList();
    }

    public List<Booking> findByUserIdNewestFirst(String userId) {
        return dynamoDbClient.scan(
                        ScanRequest.builder()
                                .tableName(tableName)
                                .filterExpression("#userId = :userId")
                                .expressionAttributeNames(
                                        Map.of("#userId", "userId")
                                )
                                .expressionAttributeValues(
                                        Map.of(
                                                ":userId",
                                                AttributeValue.builder().s(userId).build()
                                        )
                                )
                                .build()
                ).items()
                .stream()
                .map(this::fromItem)
                .sorted((first, second) ->
                        second.getCreatedAt().compareTo(first.getCreatedAt())
                )
                .toList();
    }

    public Optional<Booking> findById(String bookingId) {
        Map<String, AttributeValue> key = Map.of(
                "bookingId", AttributeValue.builder().s(bookingId).build()
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

    private Map<String, AttributeValue> toItem(Booking booking) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("bookingId", AttributeValue.builder().s(booking.getBookingId()).build());
        item.put("userId", AttributeValue.builder().s(booking.getUserId()).build());
        item.put("flightId", AttributeValue.builder().s(booking.getFlightId()).build());
        item.put("passengerName", AttributeValue.builder().s(booking.getPassengerName()).build());
        item.put("seatCount", AttributeValue.builder().n(String.valueOf(booking.getSeatCount())).build());
        item.put("totalAmount", AttributeValue.builder().n(String.valueOf(booking.getTotalAmount())).build());
        item.put("bookingStatus", AttributeValue.builder().s(booking.getBookingStatus()).build());
        item.put("paymentStatus", AttributeValue.builder().s(booking.getPaymentStatus()).build());
        item.put("createdAt", AttributeValue.builder().s(booking.getCreatedAt()).build());

        return item;
    }

    private Booking fromItem(Map<String, AttributeValue> item) {
        return new Booking(
                item.get("bookingId").s(),
                item.get("userId").s(),
                item.get("flightId").s(),
                item.get("passengerName").s(),
                Integer.parseInt(item.get("seatCount").n()),
                Double.parseDouble(item.get("totalAmount").n()),
                item.get("bookingStatus").s(),
                item.get("paymentStatus").s(),
                item.get("createdAt").s()
        );
    }
}