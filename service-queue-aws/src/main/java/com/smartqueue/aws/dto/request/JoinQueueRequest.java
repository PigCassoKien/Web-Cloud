package com.smartqueue.aws.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinQueueRequest {
    
    @NotBlank(message = "User ID is required")
    private String userId;
}