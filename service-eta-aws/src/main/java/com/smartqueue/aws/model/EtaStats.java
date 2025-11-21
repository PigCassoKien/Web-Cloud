package com.smartqueue.aws.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtaStats {
    
    private String queueId;
    private Instant windowStart;
    private Integer servedCount;
    private Double emaServiceRate;
    private Integer p90WaitTimeMinutes;
    private Integer p50WaitTimeMinutes;
    private Instant updatedAt;
    private String timeWindow;
    
    // TableStore primary key will be queueId + timeWindow
    public String getPrimaryKey() {
        return queueId + "#" + timeWindow;
    }
}