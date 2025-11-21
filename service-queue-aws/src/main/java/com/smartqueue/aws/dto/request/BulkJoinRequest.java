package com.smartqueue.aws.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkJoinRequest {
    
    @NotBlank(message = "Queue ID is required")
    private String queueId;
    
    @NotNull(message = "Batch size is required")
    @Min(value = 1, message = "Batch size must be at least 1")
    private Integer batch;
}