package com.smartqueue.aws.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueStatusResponse {
    
    private String ticketId;
    private String queueId;
    private Integer position;
    private Integer estimatedWaitMinutes;
    private String status;
    private String message;
}