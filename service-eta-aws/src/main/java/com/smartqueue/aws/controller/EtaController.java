package com.smartqueue.aws.controller;

import com.smartqueue.aws.dto.request.NotificationRequest;
import com.smartqueue.aws.dto.request.UpdateStatsRequest;
import com.smartqueue.aws.dto.response.EtaResponse;
import com.smartqueue.aws.dto.response.NotificationResponse;
import com.smartqueue.aws.service.EtaService;
import com.smartqueue.aws.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
public class EtaController {
    
    private final EtaService etaService;
    private final NotificationService notificationService;
    
    @GetMapping("/eta")
    public ResponseEntity<EtaResponse> getEta(
            @RequestParam @NotBlank String queueId,
            @RequestParam @NotBlank String ticketId,
            @RequestParam(required = false) Integer position) {
        
        log.info("ETA request received for queueId: {}, ticketId: {}, position: {}", queueId, ticketId, position);
        
        try {
            EtaResponse response = etaService.calculateEta(queueId, ticketId, position);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating ETA", e);
            return ResponseEntity.badRequest().body(
                EtaResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .estimatedWaitMinutes(10) // Fallback
                    .build()
            );
        }
    }
    
    @PostMapping("/notify")
    public ResponseEntity<NotificationResponse> sendNotification(
            @RequestBody @Valid NotificationRequest request) {
        
        log.info("Notification request received for ticketId: {}, channel: {}", 
                request.getTicketId(), request.getChannel());
        
        try {
            NotificationResponse response = notificationService.scheduleNotification(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error scheduling notification", e);
            return ResponseEntity.badRequest().body(
                NotificationResponse.builder()
                    .ticketId(request.getTicketId())
                    .scheduled(false)
                    .status("FAILED")
                    .message("Failed to schedule notification: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @PostMapping("/stats/served")
    public ResponseEntity<?> updateServedStats(
            @RequestBody @Valid UpdateStatsRequest request) {
        
        log.info("Update stats request received for queueId: {}, count: {}", 
                request.getQueueId(), request.getCount());
        
        try {
            etaService.updateServiceStats(request.getQueueId(), request.getCount(), request.getWindowSec());
            
            return ResponseEntity.ok(Map.of(
                "message", "Stats updated successfully",
                "queueId", request.getQueueId(),
                "servedCount", request.getCount()
            ));
            
        } catch (Exception e) {
            log.error("Error updating stats", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

// Thêm vào EtaController

    @GetMapping("/eta/track")
    public ResponseEntity<EtaResponse> getEtaWithTracking(
            @RequestParam @NotBlank String queueId,
            @RequestParam @NotBlank String ticketId,
            @RequestParam @NotBlank String customerEmail,
            @RequestParam(required = false) Integer position) {

        log.info("ETA tracking request for ticketId: {}, email: {}", ticketId, customerEmail);

        try {
            EtaResponse response = etaService.calculateAndTrackEta(queueId, ticketId, customerEmail, position);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in ETA tracking", e);
            return ResponseEntity.status(500).build();
        }
    }

}