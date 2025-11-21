package com.smartqueue.aws.service;

import com.smartqueue.aws.dto.request.NotificationRequest;
import com.smartqueue.aws.model.TicketEta;
import com.smartqueue.aws.repository.TicketEtaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtaSchedulerService {

    private final TicketEtaRepository ticketEtaRepository;
    private final NotificationService notificationService;

    @Value("${eta.notification.threshold-minutes:2}")
    private int notificationThresholdMinutes;

    // Direct deletion support
    private final DynamoDbClient dynamoDbClient;
    @Value("${aws.dynamodb.table-prefix:smartqueue-}")
    private String tablePrefix;

    // Use property for update cadence
    @Scheduled(fixedRateString = "${eta.scheduler.update-interval-ms:60000}")
    public void updateRemainingTime() {
        log.debug("Running scheduled ETA update");
        try {
            List<TicketEta> activeTickets = ticketEtaRepository.findAllActive();
            log.info("Processing {} active tickets", activeTickets.size());
            for (TicketEta ticket : activeTickets) {
                updateTicketEta(ticket);
            }
        } catch (Exception e) {
            log.error("Error in scheduled ETA update", e);
        }
    }

    private void updateTicketEta(TicketEta ticket) {
        try {
            long minutesPassed = Duration.between(ticket.getUpdatedAt(), Instant.now()).toMinutes();
            if (minutesPassed < 1) return;

            int newRemainingMinutes = Math.max(0, ticket.getRemainingMinutes() - (int) minutesPassed);
            ticket.setRemainingMinutes(newRemainingMinutes);
            ticket.setUpdatedAt(Instant.now());

            log.info("Updated ticket {}: {} minutes remaining", ticket.getTicketId(), newRemainingMinutes);

            if (newRemainingMinutes <= notificationThresholdMinutes
                    && newRemainingMinutes > 0
                    && !ticket.getNotificationSent()) {
                sendReadyNotification(ticket);
                ticket.setNotificationSent(true);
                ticket.setStatus(TicketEta.TicketStatus.NOTIFIED);
            }

            if (newRemainingMinutes == 0) {
                ticket.setStatus(TicketEta.TicketStatus.READY);
                try {
                    ticketEtaRepository.save(ticket); // persist final state (optional)
                } catch (Exception ignored) { }
                deleteTicketRecord(ticket.getTicketId());
                return; // skip saving after deletion
            }

            ticketEtaRepository.save(ticket);

        } catch (Exception e) {
            log.error("Error updating ticket ETA: {}", ticket.getTicketId(), e);
        }
    }

    private void sendReadyNotification(TicketEta ticket) {
        try {
            log.info("Sending ready notification to {} for ticket {}",
                    ticket.getCustomerEmail(), ticket.getTicketId());

            NotificationRequest request = NotificationRequest.builder()
                    .ticketId(ticket.getTicketId())
                    .channel(NotificationRequest.NotificationChannel.EMAIL)
                    .address(ticket.getCustomerEmail())
                    .message("Pick up time is approaching, please come to the counter to pick up your items")
                    .build();

            notificationService.scheduleNotification(request);

        } catch (Exception e) {
            log.error("Failed to send ready notification for ticket: {}", ticket.getTicketId(), e);
        }
    }

    private void deleteTicketRecord(String ticketId) {
        if (dynamoDbClient == null) {
            log.warn("DynamoDB client not available, skip delete for ticket {}", ticketId);
            return;
        }
        String tableName = tablePrefix + "ticket_eta";
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("ticketId", AttributeValue.builder().s(ticketId).build());

            dynamoDbClient.deleteItem(
                    DeleteItemRequest.builder()
                            .tableName(tableName)
                            .key(key)
                            .build()
            );
            log.info("Deleted ticket {} from table {}", ticketId, tableName);
        } catch (Exception e) {
            log.error("Failed to delete ticket {} from table {}", ticketId, tableName, e);
        }
    }
}
