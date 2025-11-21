package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TicketRepository {
    
    private final DynamoDbEnhancedClient dynamoDbClient;
    private final String ticketsTableName;
    
    private DynamoDbTable<Ticket> getTicketsTable() {
        return dynamoDbClient.table(ticketsTableName, TableSchema.fromBean(Ticket.class));
    }
    
    public Ticket save(Ticket ticket) {
        log.debug("Saving ticket: {}", ticket.getTicketId());
        
        ticket.setJoinedAt(Instant.now());
        getTicketsTable().putItem(ticket);
        
        log.info("Ticket saved successfully: {}", ticket.getTicketId());
        return ticket;
    }
    
    public Optional<Ticket> findById(String ticketId) {
        log.debug("Finding ticket by ID: {}", ticketId);
        
        try {
            Key key = Key.builder()
                    .partitionValue(ticketId)
                    .build();
            
            Ticket ticket = getTicketsTable().getItem(key);
            return Optional.ofNullable(ticket);
        } catch (Exception e) {
            log.error("Error finding ticket by ID: {}", ticketId, e);
            return Optional.empty();
        }
    }
    
    public List<Ticket> findByQueueIdAndStatus(String queueId, Ticket.TicketStatus status) {
        log.debug("Finding tickets by queue ID: {} and status: {}", queueId, status);
        
        try {
            Expression filterExpression = Expression.builder()
                    .expression("queueId = :queueId AND #status = :status")
                    .putExpressionName("#status", "status")
                    .putExpressionValue(":queueId", AttributeValue.builder().s(queueId).build())
                    .putExpressionValue(":status", AttributeValue.builder().s(status.name()).build())
                    .build();
            
            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(filterExpression)
                    .build();
            
            return getTicketsTable().scan(scanRequest)
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding tickets by queue ID and status", e);
            return List.of();
        }
    }
    
    public List<Ticket> findWaitingTicketsByQueue(String queueId) {
        return findByQueueIdAndStatus(queueId, Ticket.TicketStatus.WAITING);
    }
    
    public Ticket updateStatus(String ticketId, Ticket.TicketStatus newStatus) {
        log.debug("Updating ticket status: {} to {}", ticketId, newStatus);
        
        try {
            Optional<Ticket> existingTicket = findById(ticketId);
            if (existingTicket.isEmpty()) {
                throw new RuntimeException("Ticket not found: " + ticketId);
            }
            
            Ticket ticket = existingTicket.get();
            ticket.setStatus(newStatus);
            
            getTicketsTable().updateItem(ticket);
            
            log.info("Ticket status updated successfully: {}", ticketId);
            return ticket;
        } catch (Exception e) {
            log.error("Error updating ticket status: {}", ticketId, e);
            throw new RuntimeException("Failed to update ticket status", e);
        }
    }
    
    public void updateLastNotified(String ticketId) {
        log.debug("Updating last notified time for ticket: {}", ticketId);
        
        try {
            Optional<Ticket> existingTicket = findById(ticketId);
            if (existingTicket.isPresent()) {
                Ticket ticket = existingTicket.get();
                ticket.setLastNotifiedAt(Instant.now());
                getTicketsTable().updateItem(ticket);
                log.info("Last notified time updated for ticket: {}", ticketId);
            }
        } catch (Exception e) {
            log.error("Error updating last notified time: {}", ticketId, e);
        }
    }
    
    public int countWaitingTickets(String queueId) {
        return findWaitingTicketsByQueue(queueId).size();
    }
    
    public void deleteById(String ticketId) {
        log.debug("Deleting ticket: {}", ticketId);
        
        try {
            Key key = Key.builder()
                    .partitionValue(ticketId)
                    .build();
            
            getTicketsTable().deleteItem(key);
            log.info("Ticket deleted successfully: {}", ticketId);
        } catch (Exception e) {
            log.error("Error deleting ticket: {}", ticketId, e);
            throw new RuntimeException("Failed to delete ticket", e);
        }
    }

    // java
    public List<Ticket> findByUserId(String userId) {
        log.debug("Finding tickets by user ID: {}", userId);

        try {
            Expression filterExpression = Expression.builder()
                    .expression("userId = :userId")
                    .putExpressionValue(":userId", AttributeValue.builder().s(userId).build())
                    .build();

            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(filterExpression)
                    .build();

            return getTicketsTable().scan(scanRequest)
                    .items()
                    .stream()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error finding tickets by user ID: {}", userId, e);
            return List.of();
        }
    }

}