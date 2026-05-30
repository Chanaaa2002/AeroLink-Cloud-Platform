package com.aerolink.flightservice.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import java.util.Map;

@RestController
@RequestMapping("/health")
public class DynamoDbHealthController {

    private final DynamoDbClient dynamoDbClient;
    private final String flightsTableName;

    public DynamoDbHealthController(
            DynamoDbClient dynamoDbClient,
            @Value("${aws.dynamodb.flights-table}") String flightsTableName
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.flightsTableName = flightsTableName;
    }

    @GetMapping("/dynamodb")
    public ResponseEntity<Map<String, String>> checkDynamoDbConnection() {
        try {
            TableDescription table = dynamoDbClient.describeTable(
                    DescribeTableRequest.builder()
                            .tableName(flightsTableName)
                            .build()
            ).table();

            return ResponseEntity.ok(Map.of(
                    "service", "flight-service",
                    "database", "DynamoDB",
                    "table", table.tableName(),
                    "tableStatus", table.tableStatusAsString(),
                    "connection", "SUCCESS"
            ));

        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "service", "flight-service",
                    "database", "DynamoDB",
                    "connection", "FAILED",
                    "message", exception.getMessage()
            ));
        }
    }
}