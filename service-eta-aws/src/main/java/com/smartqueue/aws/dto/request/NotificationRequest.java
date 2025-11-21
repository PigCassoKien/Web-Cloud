package com.smartqueue.aws.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotBlank(message = "Ticket ID is required")
    private String ticketId;
    
    @NotNull(message = "Channel is required")
    private NotificationChannel channel;
    
    @NotBlank(message = "Address is required")
    private String address;
    
    private String message;
    
    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH
    }
}