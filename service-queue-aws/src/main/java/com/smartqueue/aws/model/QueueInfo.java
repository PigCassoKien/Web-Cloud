package com.smartqueue.aws.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class QueueInfo {
    
    private String queueId;
    private String queueName;
    private Integer openSlots;
    private Double serviceRateEma;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isActive;
    private Integer maxCapacity;
    
    @DynamoDbPartitionKey
    public String getQueueId() {
        return queueId;
    }
    
    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }
}