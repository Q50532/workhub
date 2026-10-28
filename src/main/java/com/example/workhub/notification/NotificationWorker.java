package com.example.workhub.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Executed on bounded worker threads; transforms a durable raw event into inbox text. */
@Component
public class NotificationWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);
    private final NotificationJobMapper mapper;

    public NotificationWorker(NotificationJobMapper mapper) { this.mapper = mapper; }

    public void process(long id) {
        if (mapper.claim(id) != 1) return; // another worker owns it
        try {
            NotificationJob job = mapper.findById(id);
            if (job == null) throw new IllegalStateException("Notification job missing: " + id);
            String message = switch (job.getEventType()) {
                case "STATUS_CHANGED" -> "Ticket #" + job.getTicketId() + " status: "
                        + job.getOldValue() + " -> " + job.getNewValue();
                case "ASSIGNED" -> "Ticket #" + job.getTicketId() + " was assigned to you";
                case "SLA_BREACH" -> "Ticket #" + job.getTicketId() + " exceeded its SLA target";
                default -> throw new IllegalArgumentException("Unknown notification type");
            };
            if (mapper.markReady(id, message) != 1)
                throw new IllegalStateException("Cannot complete notification " + id);
            log.debug("Notification {} ready for user {}", id, job.getRecipientId());
        } catch (RuntimeException e) {
            log.error("Notification {} processing failed; will retry", id, e);
            try { mapper.releaseForRetry(id); }
            catch (RuntimeException recoveryError) {
                // A stale PROCESSING lease will be reclaimed after two minutes.
                log.error("Notification {} retry release failed", id, recoveryError);
            }
        }
    }
}
