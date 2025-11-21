package com.smartqueue.aws.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessNextResponse {
    
    private Integer dequeuedCount;
    private Integer newOpenSlots;
    private String queueId;
    private String message;
}