package com.smartqueue.aws.service;

import com.smartqueue.aws.dto.request.JoinQueueRequest;
import com.smartqueue.aws.dto.request.ProcessNextRequest;
import com.smartqueue.aws.dto.response.JoinQueueResponse;
import com.smartqueue.aws.dto.response.ProcessNextResponse;
import com.smartqueue.aws.dto.response.QueueStatusResponse;
import com.smartqueue.aws.model.QueueInfo;
import com.smartqueue.aws.model.Ticket;
import com.smartqueue.aws.model.User;
import com.smartqueue.aws.repository.QueueRepository;
import com.smartqueue.aws.repository.TicketRepository;
import com.smartqueue.aws.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {
    
    private final TicketRepository ticketRepository;
    private final QueueRepository queueRepository;
    private final UserRepository userRepository;
    private final WebClient etaServiceWebClient;
    private final Duration etaServiceTimeout;
    
    public JoinQueueResponse joinQueue(String queueId, JoinQueueRequest request) {
        log.info("Processing join queue request for queueId: {} by user: {}", queueId, request.getUserId());
        
        try {
            // Get user information
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isEmpty()) {
                throw new RuntimeException("User not found: " + request.getUserId());
            }
            
            User user = userOpt.get();
            
            // Create ticket with full user information
            Ticket ticket = Ticket.builder()
                    .ticketId(Ticket.generateTicketId())
                    .queueId(queueId)
                    .status(Ticket.TicketStatus.WAITING)
                    .userId(user.getUserId())
                    .userEmail(user.getEmail())
                    .userPhone(user.getPhone())
                    .userName(user.getName())
                    .position(getNextPosition(queueId))
                    .joinedAt(Instant.now())
                    .emailNotificationEnabled(user.isEmailNotificationEnabled())
                    .smsNotificationEnabled(user.isSmsNotificationEnabled())
                    .build();
            
            // Save ticket
            ticket = ticketRepository.save(ticket);
            
            // Calculate position
            int position = calculatePosition(queueId, ticket.getTicketId());
            ticket.setPosition(position);
            ticketRepository.save(ticket);
            
            log.info("User joined queue successfully. TicketId: {}, Position: {}", ticket.getTicketId(), position);
            
            return JoinQueueResponse.builder()
                    .ticketId(ticket.getTicketId())
                    .queueId(queueId)
                    .position(position)
                    .message("Successfully joined queue")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error joining queue: {}", queueId, e);
            throw new RuntimeException("Failed to join queue", e);
        }
    }
    
    public QueueStatusResponse getQueueStatus(String queueId, String ticketId) {
        log.info("Getting queue status for queueId: {}, ticketId: {}", queueId, ticketId);
        
        try {
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (ticketOpt.isEmpty()) {
                throw new RuntimeException("Ticket not found: " + ticketId);
            }
            
            Ticket ticket = ticketOpt.get();
            if (!ticket.getQueueId().equals(queueId)) {
                throw new RuntimeException("Ticket does not belong to this queue");
            }
            
            // Calculate current position
            int currentPosition = calculatePosition(queueId, ticketId);
            
            // Get ETA from Service B
            Integer estimatedWaitMinutes = getEstimatedWaitTime(queueId, ticketId, currentPosition);
            
            return QueueStatusResponse.builder()
                    .ticketId(ticketId)
                    .queueId(queueId)
                    .position(currentPosition)
                    .estimatedWaitMinutes(estimatedWaitMinutes)
                    .status(ticket.getStatus().name())
                    .message("Queue status retrieved successfully")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error getting queue status", e);
            throw new RuntimeException("Failed to get queue status", e);
        }
    }
    
    public ProcessNextResponse processNext(String queueId, ProcessNextRequest request) {
        log.info("Processing next {} customers for queueId: {}", request.getCount(), queueId);
        
        try {
            List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueId);
            
            int processCount = Math.min(request.getCount(), waitingTickets.size());
            int processed = 0;
            
            for (int i = 0; i < processCount; i++) {
                Ticket ticket = waitingTickets.get(i);
                ticketRepository.updateStatus(ticket.getTicketId(), Ticket.TicketStatus.SERVED);
                processed++;
            }
            
            // Update open slots
            Optional<QueueInfo> queueInfoOpt = queueRepository.findById(queueId);
            int newOpenSlots = queueInfoOpt.map(QueueInfo::getOpenSlots).orElse(0) + processed;
            queueRepository.updateOpenSlots(queueId, newOpenSlots);
            
            // Notify Service B about the processing
            notifyServiceB(queueId, processed);
            
            log.info("Processed {} customers for queueId: {}", processed, queueId);
            
            return ProcessNextResponse.builder()
                    .queueId(queueId)
                    .dequeuedCount(processed)
                    .newOpenSlots(newOpenSlots)
                    .message("Successfully processed " + processed + " customers")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error processing next customers for queue: {}", queueId, e);
            throw new RuntimeException("Failed to process next customers", e);
        }
    }
    
    private int calculatePosition(String queueId, String ticketId) {
        List<Ticket> waitingTickets = ticketRepository.findWaitingTicketsByQueue(queueId);
        
        // Sort by joinedAt timestamp
        waitingTickets.sort((t1, t2) -> t1.getJoinedAt().compareTo(t2.getJoinedAt()));
        
        // Find position of current ticket
        for (int i = 0; i < waitingTickets.size(); i++) {
            if (waitingTickets.get(i).getTicketId().equals(ticketId)) {
                return i + 1; // 1-based position
            }
        }
        
        return waitingTickets.size() + 1; // If not found, put at end
    }
    
    private Integer getEstimatedWaitTime(String queueId, String ticketId, int position) {
        try {
            return etaServiceWebClient
                    .get()
                    .uri("/eta?queueId={queueId}&ticketId={ticketId}&position={position}", queueId, ticketId, position)
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .timeout(etaServiceTimeout)
                    .onErrorReturn(position * 5) // Fallback: 5 minutes per position
                    .block();
        } catch (Exception e) {
            log.warn("Failed to get ETA from service B, using fallback calculation", e);
            return position * 5; // Fallback calculation
        }
    }
    
    private void notifyServiceB(String queueId, int servedCount) {
        try {
            etaServiceWebClient
                    .post()
                    .uri("/stats/served")
                    .bodyValue(Map.of("queueId", queueId, "count", servedCount, "windowSec", 60))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(etaServiceTimeout)
                    .subscribe(
                        response -> log.debug("Successfully notified service B: {}", response),
                        error -> log.warn("Failed to notify service B", error)
                    );
        } catch (Exception e) {
            log.warn("Error notifying service B", e);
        }
    }

    private int getNextPosition(String queueId) {
        try {
            List<Ticket> waitingTickets = ticketRepository.findByQueueIdAndStatus(queueId, Ticket.TicketStatus.WAITING);
            return waitingTickets.size() + 1;
        } catch (Exception e) {
            log.error("Error calculating next position for queue: {}", queueId, e);
            return 1; // Fallback to position 1
        }
    }
}