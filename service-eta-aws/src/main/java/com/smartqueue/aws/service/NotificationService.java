package com.smartqueue.aws.service;

import com.smartqueue.aws.dto.request.NotificationRequest;
import com.smartqueue.aws.dto.response.NotificationResponse;
import com.smartqueue.aws.model.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    @Autowired(required = false)
    private SesNotificationService sesNotificationService;

    public NotificationResponse scheduleNotification(NotificationRequest request) {
        log.info("Scheduling {} notification for ticket: {}", request.getChannel(), request.getTicketId());

        try {
            NotificationLog notificationLog = NotificationLog.builder()
                    .notificationId(UUID.randomUUID().toString())
                    .ticketId(request.getTicketId())
                    .channel(convertChannel(request.getChannel()))
                    .recipient(request.getAddress())
                    .message(request.getMessage() != null ? request.getMessage() : "Your queue notification")
                    .status(NotificationLog.NotificationStatus.PENDING)
                    .scheduledAt(Instant.now())
                    .build();

            if (sesNotificationService != null && request.getChannel() == NotificationRequest.NotificationChannel.EMAIL) {
                String subject = "SmartQueue: Your order is coming";
                String body = request.getMessage() != null ? request.getMessage() :
                        "Pick up time is approaching, please come to the counter to pick up your items";

                sesNotificationService.sendEmail(request.getAddress(), subject, body);
                notificationLog.setStatus(NotificationLog.NotificationStatus.SENT);
                notificationLog.setSentAt(Instant.now());
            }

            return NotificationResponse.builder()
                    .ticketId(request.getTicketId())
                    .scheduled(true)
                    .status(notificationLog.getStatus().name())
                    .message("Notification scheduled successfully")
                    .notificationId(notificationLog.getNotificationId())
                    .build();

        } catch (Exception e) {
            log.error("Error scheduling notification for ticket: {}", request.getTicketId(), e);

            return NotificationResponse.builder()
                    .ticketId(request.getTicketId())
                    .scheduled(false)
                    .status("FAILED")
                    .message("Failed to schedule notification: " + e.getMessage())
                    .build();
        }
    }

    private NotificationLog.NotificationType convertChannel(NotificationRequest.NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> NotificationLog.NotificationType.EMAIL;
            case SMS -> NotificationLog.NotificationType.SMS;
            case PUSH -> NotificationLog.NotificationType.PUSH;
        };
    }
}
