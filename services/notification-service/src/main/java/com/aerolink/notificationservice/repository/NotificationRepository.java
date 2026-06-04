package com.aerolink.notificationservice.repository;

import com.aerolink.notificationservice.model.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class NotificationRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String userIndexName;

    public NotificationRepository(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.notifications-table}") String tableName,
            @Value("${aws.dynamodb.notifications-user-index}") String userIndexName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.userIndexName = userIndexName;
    }

    public List<Notification> findByUserIdNewestFirst(String userId) {
        Map<String, String> attributeNames = Map.of(
                "#userId", "userId"
        );

        Map<String, AttributeValue> attributeValues = Map.of(
                ":userId", AttributeValue.builder().s(userId).build()
        );

        List<Map<String, AttributeValue>> items = dynamoDbClient.query(
                QueryRequest.builder()
                        .tableName(tableName)
                        .indexName(userIndexName)
                        .keyConditionExpression("#userId = :userId")
                        .expressionAttributeNames(attributeNames)
                        .expressionAttributeValues(attributeValues)
                        .scanIndexForward(false)
                        .build()
        ).items();

        List<Notification> notifications = new ArrayList<>();

        for (Map<String, AttributeValue> item : items) {
            notifications.add(fromItem(item));
        }

        return notifications;
    }

    private Notification fromItem(Map<String, AttributeValue> item) {
        Notification notification = new Notification();

        notification.setNotificationId(getString(item, "notificationId"));
        notification.setUserId(getString(item, "userId"));
        notification.setType(getString(item, "type"));
        notification.setTitle(getString(item, "title"));
        notification.setMessage(getString(item, "message"));
        notification.setStatus(getString(item, "status"));
        notification.setCreatedAt(getString(item, "createdAt"));

        notification.setBookingId(getString(item, "bookingId"));
        notification.setPaymentId(getString(item, "paymentId"));

        notification.setBaggageId(getString(item, "baggageId"));
        notification.setBaggageStatus(getString(item, "baggageStatus"));
        notification.setCurrentLocation(getString(item, "currentLocation"));

        return notification;
    }

    private String getString(Map<String, AttributeValue> item, String attributeName) {
        AttributeValue value = item.get(attributeName);

        if (value == null || value.s() == null) {
            return null;
        }

        return value.s();
    }
}