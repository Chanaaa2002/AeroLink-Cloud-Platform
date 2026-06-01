package com.aerolink.paymentservice.repository;

import com.aerolink.paymentservice.model.Payment;
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
public class PaymentRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public PaymentRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.payments-table}") String tableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    public Payment save(Payment payment) {
        dynamoDbClient.putItem(
                PutItemRequest.builder()
                        .tableName(tableName)
                        .item(toItem(payment))
                        .build()
        );

        return payment;
    }

    public List<Payment> findAll() {
        return dynamoDbClient.scan(
                        ScanRequest.builder()
                                .tableName(tableName)
                                .build()
                ).items()
                .stream()
                .map(this::fromItem)
                .toList();
    }

    public Optional<Payment> findById(String paymentId) {
        Map<String, AttributeValue> key = Map.of(
                "paymentId", AttributeValue.builder().s(paymentId).build()
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

    private Map<String, AttributeValue> toItem(Payment payment) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put("paymentId", AttributeValue.builder().s(payment.getPaymentId()).build());
        item.put("bookingId", AttributeValue.builder().s(payment.getBookingId()).build());
        item.put("amount", AttributeValue.builder().n(String.valueOf(payment.getAmount())).build());
        item.put("currency", AttributeValue.builder().s(payment.getCurrency()).build());
        item.put("paymentMethod", AttributeValue.builder().s(payment.getPaymentMethod()).build());
        item.put("paymentStatus", AttributeValue.builder().s(payment.getPaymentStatus()).build());
        item.put("createdAt", AttributeValue.builder().s(payment.getCreatedAt()).build());

        if (payment.getProcessedAt() != null && !payment.getProcessedAt().isBlank()) {
            item.put("processedAt", AttributeValue.builder().s(payment.getProcessedAt()).build());
        }

        return item;
    }

    private Payment fromItem(Map<String, AttributeValue> item) {
        String processedAt = item.containsKey("processedAt")
                ? item.get("processedAt").s()
                : null;

        return new Payment(
                item.get("paymentId").s(),
                item.get("bookingId").s(),
                Double.parseDouble(item.get("amount").n()),
                item.get("currency").s(),
                item.get("paymentMethod").s(),
                item.get("paymentStatus").s(),
                item.get("createdAt").s(),
                processedAt
        );
    }
}