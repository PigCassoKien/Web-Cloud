package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.QueueInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class QueueRepository {
    
    private final DynamoDbEnhancedClient dynamoDbClient;
    private final String queuesTableName;
    
    private DynamoDbTable<QueueInfo> getQueuesTable() {
        return dynamoDbClient.table(queuesTableName, TableSchema.fromBean(QueueInfo.class));
    }
    
    public QueueInfo save(QueueInfo queueInfo) {
        log.debug("Saving queue info: {}", queueInfo.getQueueId());
        
        queueInfo.setUpdatedAt(Instant.now());
        if (queueInfo.getCreatedAt() == null) {
            queueInfo.setCreatedAt(Instant.now());
        }
        
        getQueuesTable().putItem(queueInfo);
        
        log.info("Queue info saved successfully: {}", queueInfo.getQueueId());
        return queueInfo;
    }
    
    public Optional<QueueInfo> findById(String queueId) {
        log.debug("Finding queue info by ID: {}", queueId);
        
        try {
            Key key = Key.builder()
                    .partitionValue(queueId)
                    .build();
            
            QueueInfo queueInfo = getQueuesTable().getItem(key);
            return Optional.ofNullable(queueInfo);
        } catch (Exception e) {
            log.error("Error finding queue info by ID: {}", queueId, e);
            return Optional.empty();
        }
    }
    
    public QueueInfo updateOpenSlots(String queueId, int newOpenSlots) {
        log.debug("Updating open slots for queue: {} to {}", queueId, newOpenSlots);
        
        try {
            Optional<QueueInfo> existingQueue = findById(queueId);
            if (existingQueue.isEmpty()) {
                // Create default queue info if not exists
                QueueInfo queueInfo = QueueInfo.builder()
                        .queueId(queueId)
                        .queueName("Queue " + queueId)
                        .openSlots(newOpenSlots)
                        .serviceRateEma(1.0) // Default service rate
                        .isActive(true)
                        .maxCapacity(100)
                        .build();
                return save(queueInfo);
            }
            
            QueueInfo queueInfo = existingQueue.get();
            queueInfo.setOpenSlots(newOpenSlots);
            queueInfo.setUpdatedAt(Instant.now());
            
            return save(queueInfo);
        } catch (Exception e) {
            log.error("Error updating open slots for queue: {}", queueId, e);
            throw new RuntimeException("Failed to update open slots", e);
        }
    }
    
    public QueueInfo updateServiceRate(String queueId, double serviceRate) {
        log.debug("Updating service rate for queue: {} to {}", queueId, serviceRate);
        
        try {
            Optional<QueueInfo> existingQueue = findById(queueId);
            if (existingQueue.isEmpty()) {
                throw new RuntimeException("Queue not found: " + queueId);
            }
            
            QueueInfo queueInfo = existingQueue.get();
            queueInfo.setServiceRateEma(serviceRate);
            queueInfo.setUpdatedAt(Instant.now());
            
            return save(queueInfo);
        } catch (Exception e) {
            log.error("Error updating service rate for queue: {}", queueId, e);
            throw new RuntimeException("Failed to update service rate", e);
        }
    }
    
    public int getOpenSlots(String queueId) {
        return findById(queueId)
                .map(QueueInfo::getOpenSlots)
                .orElse(0);
    }
    
    public double getServiceRate(String queueId) {
        return findById(queueId)
                .map(QueueInfo::getServiceRateEma)
                .orElse(1.0); // Default service rate
    }
    
    public void deleteById(String queueId) {
        log.debug("Deleting queue: {}", queueId);
        
        try {
            Key key = Key.builder()
                    .partitionValue(queueId)
                    .build();
            
            getQueuesTable().deleteItem(key);
            log.info("Queue deleted successfully: {}", queueId);
        } catch (Exception e) {
            log.error("Error deleting queue: {}", queueId, e);
            throw new RuntimeException("Failed to delete queue", e);
        }
    }

    public List<QueueInfo> findAll() {
        log.debug("Finding all queues");

        try {
            return getQueuesTable().scan()
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding all queues", e);
            return List.of();
        }
    }

}