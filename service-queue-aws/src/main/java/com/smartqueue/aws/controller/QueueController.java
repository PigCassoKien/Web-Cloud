package com.smartqueue.aws.controller;

import com.smartqueue.aws.dto.request.BulkJoinRequest;
import com.smartqueue.aws.dto.request.CreateQueueRequest;
import com.smartqueue.aws.dto.request.JoinQueueRequest;
import com.smartqueue.aws.dto.request.ProcessNextRequest;
import com.smartqueue.aws.dto.response.JoinQueueResponse;
import com.smartqueue.aws.dto.response.ProcessNextResponse;
import com.smartqueue.aws.dto.response.QueueStatusResponse;
import com.smartqueue.aws.model.QueueInfo;
import com.smartqueue.aws.model.Ticket;
import com.smartqueue.aws.repository.QueueRepository;
import com.smartqueue.aws.service.QueueService;
import com.smartqueue.aws.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Validated
@CrossOrigin(origins = "*")
public class QueueController {
    
    private final QueueService queueService;

    private final QueueRepository queueRepository;

    @Value("${test.api-key}")
    private String testApiKey;
    
    @PostMapping("/{queueId}/join")
    public ResponseEntity<JoinQueueResponse> joinQueue(
            @PathVariable @NotBlank String queueId,
            @RequestBody @Valid JoinQueueRequest request) {
        
        log.info("Join queue request received for queueId: {}", queueId);
        
        try {
            JoinQueueResponse response = queueService.joinQueue(queueId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in join queue", e);
            return ResponseEntity.badRequest().body(
                JoinQueueResponse.builder()
                    .queueId(queueId)
                    .message("Failed to join queue: " + e.getMessage())
                    .build()
            );
        }
    }
    
    @GetMapping("/{queueId}/status")
    public ResponseEntity<QueueStatusResponse> getStatus(
            @PathVariable @NotBlank String queueId,
            @RequestParam @NotBlank String ticketId) {
        
        log.info("Status request received for queueId: {}, ticketId: {}", queueId, ticketId);
        
        try {
            QueueStatusResponse response = queueService.getQueueStatus(queueId, ticketId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting queue status", e);
            return ResponseEntity.badRequest().body(
                QueueStatusResponse.builder()
                    .queueId(queueId)
                    .ticketId(ticketId)
                    .message("Failed to get status: " + e.getMessage())
                    .build()
            );
        }
    }

    @PostMapping
    public ResponseEntity<QueueInfo> createQueue(@RequestBody @Valid CreateQueueRequest request) {
        log.info("Creating new queue: {}", request.getQueueId());

        try {
            QueueInfo queueInfo = QueueInfo.builder()
                    .queueId(request.getQueueId())
                    .queueName(request.getQueueName())
                    .openSlots(request.getMaxCapacity())
                    .maxCapacity(request.getMaxCapacity())
                    .serviceRateEma(1.0)
                    .isActive(true)
                    .build();

            QueueInfo saved = queueRepository.save(queueInfo);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error creating queue", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{queueId}/next")
    public ResponseEntity<ProcessNextResponse> processNext(
            @PathVariable @NotBlank String queueId,
            @RequestBody @Valid ProcessNextRequest request) {
        
        log.info("Process next request received for queueId: {}, count: {}", queueId, request.getCount());
        
        try {
            ProcessNextResponse response = queueService.processNext(queueId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing next", e);
            return ResponseEntity.badRequest().body(
                ProcessNextResponse.builder()
                    .queueId(queueId)
                    .message("Failed to process next: " + e.getMessage())
                    .build()
            );
        }
    }
    
    // Test endpoint for load testing
    @PostMapping("/test/join-bulk")
    public ResponseEntity<?> joinBulk(
            @RequestHeader(value = "X-Test-Key", required = false) String testKey,
            @RequestBody @Valid BulkJoinRequest request) {
        
        log.info("Bulk join request received for queueId: {}, batch: {}", request.getQueueId(), request.getBatch());
        
        // Validate test key
        if (testKey == null || !testKey.equals(testApiKey)) {
            return ResponseEntity.status(403).body(Map.of("error", "Invalid test key"));
        }
        
        try {
            List<JoinQueueResponse> responses = new ArrayList<>();
            
            for (int i = 0; i < request.getBatch(); i++) {
                JoinQueueRequest joinRequest = JoinQueueRequest.builder()
                        .userId("test-user-" + i)
                        .build();
                
                JoinQueueResponse response = queueService.joinQueue(request.getQueueId(), joinRequest);
                responses.add(response);
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Bulk join completed",
                "processed", request.getBatch(),
                "responses", responses
            ));
            
        } catch (Exception e) {
            log.error("Error in bulk join", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<QueueInfo>> getAllQueues() {
        log.info("Get all queues request received");
        try {
            List<QueueInfo> queues = queueRepository.findAll();
            return ResponseEntity.ok(queues);
        } catch (Exception e) {
            log.error("Error getting all queues", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



}