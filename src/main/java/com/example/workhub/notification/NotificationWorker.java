package com.example.workhub.notification;

import java.util.UUID;
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
        String claimToken = UUID.randomUUID().toString();
        if (mapper.claim(id, claimToken) != 1) return; // another worker owns it
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
            if (mapper.markReady(id, claimToken, message) != 1) {
                // The lease may have expired and been claimed by another worker.
                // Do not release another worker's claim.
                log.warn("Notification {} completion ignored: worker no longer owns the claim", id);
                return;
            }
            log.debug("Notification {} ready for user {}", id, job.getRecipientId());
        } catch (RuntimeException e) {
            log.error("Notification {} processing failed", id, e);
            try {
                if (mapper.releaseForRetry(id, claimToken) != 1) {
                    log.warn("Notification {} release skipped: claim was superseded or job completed", id);
                }
            } catch (RuntimeException recoveryError) {
                // The expired PROCESSING lease will be reclaimed or marked FAILED.
                log.error("Notification {} retry release failed", id, recoveryError);
            }
        }
    }
}
