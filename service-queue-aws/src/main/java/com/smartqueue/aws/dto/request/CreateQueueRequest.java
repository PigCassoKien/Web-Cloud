package com.smartqueue.aws.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQueueRequest {

    @NotBlank(message = "Queue ID is required")
    @Size(min = 2, max = 50, message = "Queue ID must be between 2 and 50 characters")
    private String queueId;

    @NotBlank(message = "Queue name is required")
    @Size(min = 2, max = 100, message = "Queue name must be between 2 and 100 characters")
    private String queueName;

    @NotNull(message = "Max capacity is required")
    @Min(value = 1, message = "Max capacity must be at least 1")
    private Integer maxCapacity;
}
