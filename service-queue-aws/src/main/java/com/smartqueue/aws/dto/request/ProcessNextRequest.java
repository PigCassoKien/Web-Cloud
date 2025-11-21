package com.smartqueue.aws.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessNextRequest {
    
    @NotNull(message = "Count is required")
    @Min(value = 1, message = "Count must be at least 1")
    private Integer count;
}