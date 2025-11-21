package com.smartqueue.aws.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaResponse {
    
    private String queueId;
    private String ticketId;
    private Integer estimatedWaitMinutes;
    private Integer p90WaitMinutes;
    private Integer p50WaitMinutes;
    private Double serviceRate;
    private Instant updatedAt;
    private Integer remainingMinutes; // ✅ Thêm field này

}