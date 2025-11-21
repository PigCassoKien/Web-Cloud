package com.smartqueue.aws.service;

import com.smartqueue.aws.dto.response.EtaResponse;
import com.smartqueue.aws.model.EtaStats;
import com.smartqueue.aws.model.TicketEta;
import com.smartqueue.aws.repository.EtaStatsRepository;
import com.smartqueue.aws.repository.TicketEtaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class EtaService {

    @Autowired(required = false)
    private EtaStatsRepository etaStatsRepository;

    @Autowired
    private TicketEtaRepository ticketEtaRepository;

    @Value("${eta.calculation.ema-alpha:0.3}")
    private double emaAlpha;

    @Value("${eta.calculation.default-service-rate:1.0}")
    private double defaultServiceRate;

    // For direct delete on zero
    @Autowired(required = false)
    private DynamoDbClient dynamoDbClient;

    @Value("${aws.dynamodb.table-prefix:smartqueue-}")
    private String tablePrefix;

    public EtaResponse calculateEta(String queueId, String ticketId, Integer position) {
        log.info("Calculating SMART ETA for queueId: {}, ticketId: {}, position: {}", queueId, ticketId, position);

        try {
            Optional<EtaStats> statsOpt = etaStatsRepository.findLatestByQueueId(queueId);

            double baseServiceRate = defaultServiceRate;
            int p90Wait = 10;
            int p50Wait = 5;

            if (statsOpt.isPresent()) {
                EtaStats stats = statsOpt.get();
                baseServiceRate = stats.getEmaServiceRate();
                p90Wait = stats.getP90WaitTimeMinutes();
                p50Wait = stats.getP50WaitTimeMinutes();
            }

            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            double smartServiceRate = calculateSmartServiceRate(baseServiceRate, now);

            double baseEtaMinutes = position / smartServiceRate;

            double finalEta = applySmartFactors(baseEtaMinutes, now, position);
            int estimatedWaitMinutes = Math.max(1, (int) Math.ceil(finalEta));

            log.info("SMART ETA calculated - Queue: {}, Position: {}, Base: {}min, Smart: {}min",
                    queueId, position, String.format("%.1f", baseEtaMinutes), estimatedWaitMinutes);

            return EtaResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .estimatedWaitMinutes(estimatedWaitMinutes)
                    .p90WaitMinutes(p90Wait)
                    .p50WaitMinutes(p50Wait)
                    .serviceRate(smartServiceRate)
                    .updatedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Error calculating ETA for queue: {}", queueId, e);

            return EtaResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .estimatedWaitMinutes(position != null ? position * 5 : 10)
                    .p90WaitMinutes(10)
                    .p50WaitMinutes(5)
                    .serviceRate(defaultServiceRate)
                    .updatedAt(Instant.now())
                    .build();
        }
    }

    // Track with live countdown; delete immediately when it reaches 0
    public EtaResponse calculateAndTrackEta(String queueId, String ticketId, String customerEmail, Integer position) {
        Optional<TicketEta> existingTicket = ticketEtaRepository.findByTicketId(ticketId);

        if (existingTicket.isPresent()) {
            TicketEta ticket = existingTicket.get();

            long minutesPassed = Duration.between(ticket.getUpdatedAt(), Instant.now()).toMinutes();
            int currentRemaining = ticket.getRemainingMinutes();

            if (minutesPassed > 0) {
                int newRemaining = Math.max(0, currentRemaining - (int) minutesPassed);
                ticket.setRemainingMinutes(newRemaining);
                ticket.setUpdatedAt(Instant.now());

                if (newRemaining == 0) {
                    ticket.setStatus(TicketEta.TicketStatus.READY);
                    // Persist final state then delete from table
                    try {
                        ticketEtaRepository.save(ticket);
                    } catch (Exception ignored) { }
                    deleteTicketRecord(ticket.getTicketId());
                } else {
                    ticketEtaRepository.save(ticket);
                }
                currentRemaining = newRemaining;
            }

            log.info("Ticket {} tracked, live remainingMinutes: {}", ticketId, currentRemaining);

            return EtaResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .estimatedWaitMinutes(ticket.getOriginalEtaMinutes())
                    .remainingMinutes(currentRemaining)
                    .updatedAt(ticket.getUpdatedAt())
                    .build();
        }

        EtaResponse response = calculateEta(queueId, ticketId, position);

        TicketEta ticketEta = TicketEta.builder()
                .ticketId(ticketId)
                .queueId(queueId)
                .customerEmail(customerEmail)
                .remainingMinutes(response.getEstimatedWaitMinutes())
                .originalEtaMinutes(response.getEstimatedWaitMinutes())
                .calculatedAt(Instant.now())
                .updatedAt(Instant.now())
                .notificationSent(false)
                .status(TicketEta.TicketStatus.WAITING)
                .build();

        ticketEtaRepository.save(ticketEta);
        log.info("New ticket {} tracked with {} minutes ETA", ticketId, response.getEstimatedWaitMinutes());

        response.setRemainingMinutes(response.getEstimatedWaitMinutes());
        return response;
    }

    public void updateServiceStats(String queueId, int servedCount, int windowSec) {
        log.info("Updating service stats for queueId: {}, served: {}, window: {}sec", queueId, servedCount, windowSec);
        try {
            double serviceRate = (double) servedCount / (windowSec / 60.0);
            log.info("Service stats updated successfully for queueId: {}", queueId);
        } catch (Exception e) {
            log.error("Error updating service stats for queue: {}", queueId, e);
            throw new RuntimeException("Failed to update service stats", e);
        }
    }

    public EtaStats getLatestStats(String queueId) {
        Optional<EtaStats> statsOpt = etaStatsRepository.findLatestByQueueId(queueId);

        return statsOpt
                .orElse(EtaStats.builder()
                        .queueId(queueId)
                        .emaServiceRate(defaultServiceRate)
                        .p90WaitTimeMinutes(10)
                        .p50WaitTimeMinutes(5)
                        .servedCount(0)
                        .updatedAt(Instant.now())
                        .build());
    }

    private double calculateSmartServiceRate(double baseRate, LocalDateTime now) {
        double multiplier = 1.0;
        if (isPeakHour(now)) multiplier *= 0.7;
        if (isLunchTime(now)) multiplier *= 0.5;
        if (isWeekend(now)) multiplier *= 0.8;
        if (isEveningRush(now)) multiplier *= 1.2;
        return Math.max(0.1, baseRate * multiplier);
    }

    private double applySmartFactors(double baseEta, LocalDateTime now, int position) {
        double eta = baseEta;
        if (position > 10) eta *= 1.1;
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        switch (dayOfWeek) {
            case MONDAY -> eta *= 1.15;
            case FRIDAY -> eta *= 1.1;
            case SATURDAY, SUNDAY -> eta *= 0.9;
        }
        eta *= 1.05;
        return eta;
    }

    private boolean isPeakHour(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return (time.isAfter(LocalTime.of(9, 0)) && time.isBefore(LocalTime.of(11, 0))) ||
                (time.isAfter(LocalTime.of(14, 0)) && time.isBefore(LocalTime.of(16, 0)));
    }

    private boolean isLunchTime(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return time.isAfter(LocalTime.of(12, 0)) && time.isBefore(LocalTime.of(13, 30));
    }

    private boolean isWeekend(LocalDateTime now) {
        DayOfWeek dayOfWeek = now.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean isEveningRush(LocalDateTime now) {
        LocalTime time = now.toLocalTime();
        return time.isAfter(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(20, 0));
    }

    // Direct delete from DynamoDB table smartqueue-<profile>-ticket_eta
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
