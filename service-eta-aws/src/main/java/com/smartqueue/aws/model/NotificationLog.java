package com.smartqueue.aws.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationLog {
    
    private String notificationId;
    private String ticketId;
    private String queueId;
    private NotificationType channel;
    private String recipient;
    private NotificationStatus status;
    private String message;
    private Instant sentAt;
    private Instant scheduledAt;
    private String errorMessage;
    
    public enum NotificationType {
        EMAIL,
        SMS,
        PUSH
    }
    
    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED,
        CANCELLED
    }
}