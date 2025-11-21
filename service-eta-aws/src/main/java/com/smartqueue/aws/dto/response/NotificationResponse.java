package com.smartqueue.aws.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    
    private String notificationId;
    private String ticketId;
    private Boolean scheduled;
    private String status;
    private String message;
}