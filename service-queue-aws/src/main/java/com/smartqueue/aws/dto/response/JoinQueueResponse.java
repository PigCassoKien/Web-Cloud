package com.smartqueue.aws.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinQueueResponse {
    
    private String ticketId;
    private Integer position;
    private String queueId;
    private String message;
}