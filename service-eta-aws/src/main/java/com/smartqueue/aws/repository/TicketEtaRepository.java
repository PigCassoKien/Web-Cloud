// src/main/java/com/smartqueue/aws/repository/TicketEtaRepository.java
package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.TicketEta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TicketEtaRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public TicketEtaRepository(DynamoDbClient dynamoDbClient,
                               @Value("${aws.dynamodb.table-prefix:smartqueue-}") String tablePrefix) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tablePrefix + "ticket_eta";
    }

    // Save or update a ticket ETA record
    public TicketEta save(TicketEta ticket) {
        if (dynamoDbClient == null) {
            log.warn("DynamoDB client is null, skip save for ticket {}", ticket.getTicketId());
            return ticket;
        }
        try {
            Map<String, AttributeValue> item = toItem(ticket);
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build());
            log.info("Saved ticket {} to table {}", ticket.getTicketId(), tableName);
            return ticket;
        } catch (Exception e) {
            log.error("Failed to save ticket {} to table {}", ticket.getTicketId(), tableName, e);
            throw new RuntimeException("Failed to save TicketEta", e);
        }
    }

    // Get one by ticketId
    public Optional<TicketEta> findByTicketId(String ticketId) {
        if (dynamoDbClient == null) {
            log.warn("DynamoDB client is null, skip findByTicketId for {}", ticketId);
            return Optional.empty();
        }
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("ticketId", AttributeValue.builder().s(ticketId).build());

            GetItemResponse resp = dynamoDbClient.getItem(
                    GetItemRequest.builder()
                            .tableName(tableName)
                            .key(key)
                            .build()
            );
            if (resp.hasItem() && !resp.item().isEmpty()) {
                return Optional.of(fromItem(resp.item()));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get ticket {} from table {}", ticketId, tableName, e);
            return Optional.empty();
        }
    }

    // Scan all active tickets (remainingMinutes > 0)
    public List<TicketEta> findAllActive() {
        if (dynamoDbClient == null) {
            log.warn("DynamoDB client is null, returning empty active list");
            return Collections.emptyList();
        }
        try {
            ScanRequest scan = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("remainingMinutes > :zero")
                    .expressionAttributeValues(Map.of(
                            ":zero", AttributeValue.builder().n("0").build()
                    ))
                    .build();

            ScanResponse resp = dynamoDbClient.scan(scan);
            if (!resp.hasItems()) return Collections.emptyList();

            return resp.items().stream()
                    .map(this::fromItem)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to scan active tickets in table {}", tableName, e);
            return Collections.emptyList();
        }
    }

    public void deleteByTicketId(String ticketId) {
        if (dynamoDbClient == null) {
            log.warn("DynamoDB client is null, skip delete for ticket {}", ticketId);
            return;
        }
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("ticketId", AttributeValue.builder().s(ticketId).build());

            dynamoDbClient.deleteItem(
                    DeleteItemRequest.builder()
                            .tableName(tableName)
                            .key(key)
                            .build()
            );
            log.info("Deleted ticket {} from table {}", ticketId, tableName);
        } catch (Exception e) {
            log.error("Failed to delete ticket {} from table {}", ticketId, tableName, e);
        }
    }

    // --- Mapping helpers ---

    private Map<String, AttributeValue> toItem(TicketEta t) {
        Map<String, AttributeValue> item = new HashMap<>();
        // Required PK
        item.put("ticketId", AttributeValue.builder().s(t.getTicketId()).build());

        // Strings
        putIfNotNull(item, "queueId", t.getQueueId());
        putIfNotNull(item, "customerEmail", t.getCustomerEmail());

        // Numbers
        putNumberIfNotNull(item, "remainingMinutes", t.getRemainingMinutes());
        putNumberIfNotNull(item, "originalEtaMinutes", t.getOriginalEtaMinutes());
        putEpochIfNotNull(item, "calculatedAt", t.getCalculatedAt());
        putEpochIfNotNull(item, "updatedAt", t.getUpdatedAt());

        // Bools
        if (t.getNotificationSent() != null) {
            item.put("notificationSent", AttributeValue.builder().bool(t.getNotificationSent()).build());
        }

        // Status
        if (t.getStatus() != null) {
            item.put("status", AttributeValue.builder().s(t.getStatus().name()).build());
        }

        return item;
    }

    private TicketEta fromItem(Map<String, AttributeValue> item) {
        TicketEta.TicketStatus status = null;
        if (item.containsKey("status") && item.get("status").s() != null) {
            try {
                status = TicketEta.TicketStatus.valueOf(item.get("status").s());
            } catch (IllegalArgumentException ignored) {}
        }

        Integer remaining = getInt(item, "remainingMinutes");
        Integer original = getInt(item, "originalEtaMinutes");

        return TicketEta.builder()
                .ticketId(getStr(item, "ticketId"))
                .queueId(getStr(item, "queueId"))
                .customerEmail(getStr(item, "customerEmail"))
                .remainingMinutes(remaining)
                .originalEtaMinutes(original)
                .calculatedAt(getInstant(item, "calculatedAt"))
                .updatedAt(getInstant(item, "updatedAt"))
                .notificationSent(getBool(item, "notificationSent"))
                .status(status != null ? status : TicketEta.TicketStatus.WAITING)
                .build();
    }

    private static void putIfNotNull(Map<String, AttributeValue> item, String key, String value) {
        if (value != null) item.put(key, AttributeValue.builder().s(value).build());
    }

    private static void putNumberIfNotNull(Map<String, AttributeValue> item, String key, Integer value) {
        if (value != null) item.put(key, AttributeValue.builder().n(String.valueOf(value)).build());
    }

    private static void putEpochIfNotNull(Map<String, AttributeValue> item, String key, Instant value) {
        if (value != null) item.put(key, AttributeValue.builder().n(String.valueOf(value.toEpochMilli())).build());
    }

    private static String getStr(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) && item.get(key).s() != null ? item.get(key).s() : null;
    }

    private static Integer getInt(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) && item.get(key).n() != null ? Integer.parseInt(item.get(key).n()) : null;
    }

    private static Instant getInstant(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) && item.get(key).n() != null
                ? Instant.ofEpochMilli(Long.parseLong(item.get(key).n()))
                : null;
    }

    private static Boolean getBool(Map<String, AttributeValue> item, String key) {
        return item.containsKey(key) && item.get(key).bool() != null ? item.get(key).bool() : null;
    }
}
