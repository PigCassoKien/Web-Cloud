package com.smartqueue.aws.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Ticket {
    
    private String ticketId;
    private String queueId;
    private String userId;
    private String userEmail;
    private String userPhone;
    private String userName;
    private TicketStatus status;
    private int position;
    private Instant joinedAt;
    private Instant updatedAt;
    private Instant servedAt;
    private Instant lastNotifiedAt;
    private int notificationCount;
    private double estimatedWaitMinutes;
    private boolean emailNotificationEnabled;
    private boolean smsNotificationEnabled;
    
    @DynamoDbPartitionKey
    public String getTicketId() {
        return ticketId;
    }
    
    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }
    
    @DynamoDbAttribute("userEmail")
    public String getUserEmail() {
        return userEmail;
    }
    
    @DynamoDbAttribute("userPhone")
    public String getUserPhone() {
        return userPhone;
    }
    
    @DynamoDbAttribute("userName")
    public String getUserName() {
        return userName;
    }
    
    @DynamoDbAttribute("position")
    public int getPosition() {
        return position;
    }
    
    @DynamoDbAttribute("emailNotificationEnabled")
    public boolean isEmailNotificationEnabled() {
        return emailNotificationEnabled;
    }
    
    @DynamoDbAttribute("smsNotificationEnabled")
    public boolean isSmsNotificationEnabled() {
        return smsNotificationEnabled;
    }

    public static String generateTicketId() {
        return UUID.randomUUID().toString();
    }
    
    public enum TicketStatus {
        WAITING,
        SERVED,
        CANCELLED,
        EXPIRED,
        NOTIFIED
    }
}