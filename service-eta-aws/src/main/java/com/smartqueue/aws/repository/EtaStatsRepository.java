package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.EtaStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Repository
public class EtaStatsRepository {

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;
    private final String tablePrefix;

    private static final DateTimeFormatter TIME_WINDOW_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH").withZone(ZoneOffset.UTC);

    public EtaStatsRepository(@Autowired(required = false) DynamoDbClient dynamoDbClient,
                              @Value("${aws.dynamodb.table-prefix:smartqueue-}") String tablePrefix) {
        this.dynamoDbClient = dynamoDbClient;
        this.tablePrefix = tablePrefix;
        this.tableName = tablePrefix + "eta_stats";
        log.info("EtaStatsRepository initialized with client: {}", dynamoDbClient != null ? "REAL" : "NULL");
        ensureTableExists();
    }

    public EtaStats save(EtaStats etaStats) {
        log.debug("Saving ETA stats for queue: {}", etaStats.getQueueId());

        if (dynamoDbClient == null) {
            log.warn("DynamoDB client is null, skipping save operation");
            etaStats.setUpdatedAt(Instant.now());
            return etaStats;
        }

        try {
            etaStats.setUpdatedAt(Instant.now());

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("queueId", AttributeValue.builder().s(etaStats.getQueueId()).build());
            item.put("timeWindow", AttributeValue.builder().s(etaStats.getTimeWindow()).build());
            item.put("servedCount", AttributeValue.builder().n(String.valueOf(etaStats.getServedCount())).build());
            item.put("emaServiceRate", AttributeValue.builder().n(String.valueOf(etaStats.getEmaServiceRate())).build());
            item.put("p90WaitTimeMinutes", AttributeValue.builder().n(String.valueOf(etaStats.getP90WaitTimeMinutes())).build());
            item.put("p50WaitTimeMinutes", AttributeValue.builder().n(String.valueOf(etaStats.getP50WaitTimeMinutes())).build());
            item.put("windowStart", AttributeValue.builder().n(String.valueOf(etaStats.getWindowStart().toEpochMilli())).build());
            item.put("updatedAt", AttributeValue.builder().n(String.valueOf(etaStats.getUpdatedAt().toEpochMilli())).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            dynamoDbClient.putItem(request);
            log.info("ETA stats saved successfully for queue: {}", etaStats.getQueueId());
            return etaStats;

        } catch (Exception e) {
            log.error("Error saving ETA stats for queue: {}", etaStats.getQueueId(), e);
            throw new RuntimeException("Failed to save ETA stats", e);
        }
    }

    public Optional<EtaStats> findByQueueIdAndTimeWindow(String queueId, String timeWindow) {
        log.debug("Finding ETA stats for queue: {} and time window: {}", queueId, timeWindow);

        if (dynamoDbClient == null) {
            log.warn("DynamoDB client is null, returning empty result");
            return Optional.empty();
        }

        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("queueId", AttributeValue.builder().s(queueId).build());
            key.put("timeWindow", AttributeValue.builder().s(timeWindow).build());

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();

            GetItemResponse response = dynamoDbClient.getItem(request);
            if (response.hasItem()) {
                return Optional.of(mapItemToEtaStats(response.item(), queueId, timeWindow));
            }

            return Optional.empty();

        } catch (Exception e) {
            log.error("Error finding ETA stats for queue: {} and time window: {}", queueId, timeWindow, e);
            return Optional.empty();
        }
    }

    public Optional<EtaStats> findLatestByQueueId(String queueId) {
        log.debug("Finding latest ETA stats for queue: {}", queueId);
        String currentTimeWindow = getCurrentTimeWindow();
        return findByQueueIdAndTimeWindow(queueId, currentTimeWindow);
    }

    public EtaStats updateServiceRate(String queueId, double newServiceRate, double alpha) {
        log.debug("Updating service rate for queue: {} to {} with alpha: {}", queueId, newServiceRate, alpha);

        String timeWindow = getCurrentTimeWindow();
        Optional<EtaStats> existingStats = findByQueueIdAndTimeWindow(queueId, timeWindow);

        EtaStats etaStats;
        if (existingStats.isPresent()) {
            etaStats = existingStats.get();
            double oldEma = etaStats.getEmaServiceRate();
            double newEma = alpha * newServiceRate + (1 - alpha) * oldEma;
            etaStats.setEmaServiceRate(newEma);
            etaStats.setServedCount(etaStats.getServedCount() + 1);
        } else {
            etaStats = EtaStats.builder()
                    .queueId(queueId)
                    .timeWindow(timeWindow)
                    .windowStart(Instant.now())
                    .servedCount(1)
                    .emaServiceRate(newServiceRate)
                    .p90WaitTimeMinutes(5)
                    .p50WaitTimeMinutes(3)
                    .build();
        }

        return save(etaStats);
    }

    public void dEleteByQueueId(String queueId) {
        log.debug("Deleting ETA stats for queue: {}", queueId);

        if (dynamoDbClient == null) {
            log.warn("DynamoDB client is null, skipping delete operation");
            return;
        }

        try {
            String currentTimeWindow = getCurrentTimeWindow();
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("queueId", AttributeValue.builder().s(queueId).build());
            key.put("timeWindow", AttributeValue.builder().s(currentTimeWindow).build());

            DeleteItemRequest request = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();

            dynamoDbClient.deleteItem(request);
            log.info("ETA stats deleted successfully for queue: {}", queueId);

        } catch (Exception e) {
            log.error("Error deleting ETA stats for queue: {}", queueId, e);
            throw new RuntimeException("Failed to delete ETA stats", e);
        }
    }

    private EtaStats mapItemToEtaStats(Map<String, AttributeValue> item, String queueId, String timeWindow) {
        return EtaStats.builder()
                .queueId(queueId)
                .timeWindow(timeWindow)
                .servedCount(Integer.parseInt(item.get("servedCount").n()))
                .emaServiceRate(Double.parseDouble(item.get("emaServiceRate").n()))
                .p90WaitTimeMinutes(Integer.parseInt(item.get("p90WaitTimeMinutes").n()))
                .p50WaitTimeMinutes(Integer.parseInt(item.get("p50WaitTimeMinutes").n()))
                .windowStart(Instant.ofEpochMilli(Long.parseLong(item.get("windowStart").n())))
                .updatedAt(Instant.ofEpochMilli(Long.parseLong(item.get("updatedAt").n())))
                .build();
    }

    private String getCurrentTimeWindow() {
        return TIME_WINDOW_FORMATTER.format(Instant.now());
    }

    // Tự động tạo bảng nếu chưa tồn tại (chỉ chạy 1 lần)
    private void ensureTableExists() {
        if (dynamoDbClient == null) return;

        try {
            dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("DynamoDB table {} already exists", tableName);
        } catch (ResourceNotFoundException e) {
            log.info("Table {} not found, creating...", tableName);
            createTable();
        } catch (SdkClientException e) {
            log.warn("Could not check table existence (possibly local dev): {}", e.getMessage());
        }
    }

    private void createTable() {
        try {
            CreateTableRequest request = CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(
                            KeySchemaElement.builder().attributeName("queueId").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("timeWindow").keyType(KeyType.RANGE).build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("queueId").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("timeWindow").attributeType(ScalarAttributeType.S).build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build();

            dynamoDbClient.createTable(request);
            log.info("DynamoDB table {} created successfully", tableName);

            // Wait for table to become active
            dynamoDbClient.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("Table {} is now active", tableName);

        } catch (Exception e) {
            log.error("Failed to create DynamoDB table {}", tableName, e);
        }
    }
}