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
public class TicketEta {
    private String ticketId;
    private String queueId;
    private String customerEmail;
    private Integer remainingMinutes;
    private Integer originalEtaMinutes;
    private Instant calculatedAt;
    private Instant updatedAt;
    private Boolean notificationSent;
    private TicketStatus status;

    public enum TicketStatus {
        WAITING,
        NOTIFIED,
        READY,
        COMPLETED,
        CANCELLED
    }
}
