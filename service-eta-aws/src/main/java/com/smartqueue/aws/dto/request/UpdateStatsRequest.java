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
public class UpdateStatsRequest {
    
    @NotBlank(message = "Queue ID is required")
    private String queueId;
    
    @NotNull(message = "Count is required")
    @Min(value = 1, message = "Count must be at least 1")
    private Integer count;
    
    @NotNull(message = "Window size is required")
    @Min(value = 1, message = "Window size must be at least 1")
    private Integer windowSec;
}