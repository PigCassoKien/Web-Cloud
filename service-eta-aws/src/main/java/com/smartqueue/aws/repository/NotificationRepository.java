package com.smartqueue.aws.repository;

import com.smartqueue.aws.model.NotificationLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Repository
public class NotificationRepository {
    
    private final ConcurrentMap<String, NotificationLog> notificationLogs = new ConcurrentHashMap<>();
    
    public NotificationLog save(NotificationLog notificationLog) {
        if (notificationLog.getNotificationId() == null) {
            notificationLog.setNotificationId(UUID.randomUUID().toString());
        }
        notificationLog.setScheduledAt(Instant.now());
        notificationLogs.put(notificationLog.getNotificationId(), notificationLog);
        log.info("Notification saved: {}", notificationLog.getNotificationId());
        return notificationLog;
    }
    
    public void enqueueNotification(NotificationLog notificationLog) {
        notificationLog.setStatus(NotificationLog.NotificationStatus.PENDING);
        save(notificationLog);
        log.info("Notification enqueued: {}", notificationLog.getNotificationId());
    }
    
    public void updateNotificationStatus(String notificationId, 
                                       NotificationLog.NotificationStatus status, 
                                       String errorMessage) {
        NotificationLog notificationLog = notificationLogs.get(notificationId);
        if (notificationLog != null) {
            notificationLog.setStatus(status);
            if (errorMessage != null) {
                notificationLog.setErrorMessage(errorMessage);
            }
            if (status == NotificationLog.NotificationStatus.SENT) {
                notificationLog.setSentAt(Instant.now());
            }
            save(notificationLog);
        }
    }
}
