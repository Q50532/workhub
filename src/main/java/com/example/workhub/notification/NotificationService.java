package com.example.workhub.notification;

import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Durable notification outbox: insert in Ticket TX, dispatch only after commit. */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationJobMapper mapper;
    private final NotificationWorker worker;
    private final ThreadPoolTaskExecutor executor;

    public NotificationService(NotificationJobMapper mapper, NotificationWorker worker,
            @Qualifier("ticketNotificationExecutor") ThreadPoolTaskExecutor executor) {
        this.mapper = mapper;
        this.worker = worker;
        this.executor = executor;
    }

    /** Must be invoked from within TicketService's @Transactional write. */
    public void queueStatusChange(int ticketId, int creatorId, Integer assigneeId,
            int actorId, String oldStatus, String newStatus) {
        Set<Integer> recipients = new LinkedHashSet<>();
        if (creatorId > 0 && creatorId != actorId) recipients.add(creatorId);
        if (assigneeId != null && assigneeId > 0 && assigneeId != actorId)
            recipients.add(assigneeId);
        for (int recipient : recipients) {
            queue(ticketId, recipient, actorId, "STATUS_CHANGED", oldStatus, newStatus);
        }
    }

    public void queueAssignment(int ticketId, Integer assigneeId, int actorId) {
        if (assigneeId != null && assigneeId > 0 && assigneeId != actorId)
            queue(ticketId, assigneeId, actorId, "ASSIGNED", null, null);
    }

    /** System-originated SLA notification, actor column uses recipient to satisfy existing FK.
     *  The event_type makes clear this is NOT a user-initiated change. */
    public void queueSlaEscalation(int ticketId, int managerId) {
        queue(ticketId, managerId, managerId, "SLA_BREACH", null, null);
    }

    private void queue(int ticketId, int recipient, int actor, String type,
                       String previous, String next) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Notification outbox must be written in a Ticket transaction");
        }
        NotificationJob job = new NotificationJob();
        job.setTicketId(ticketId);
        job.setRecipientId(recipient);
        job.setActorId(actor);
        job.setEventType(type);
        job.setOldValue(previous);
        job.setNewValue(next);
        mapper.add(job); // same SQL transaction as Ticket update
        if (job.getId() == null) throw new IllegalStateException("No generated notification id");
        long id = job.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { schedule(id); }
        });
    }

    private void schedule(long id) {
        try { executor.execute(() -> worker.process(id)); }
        catch (RuntimeException rejected) {
            // Crucial: do NOT turn a committed Ticket update into an apparent HTTP failure.
            // The PENDING row is durable; the scheduled recovery will retry it.
            log.warn("Async executor rejected notification {}; will retry", id, rejected);
        }
    }

    @Scheduled(fixedDelayString = "${workhub.notifications.retry-ms:10000}",
               initialDelayString = "${workhub.notifications.retry-ms:10000}")
    public void recoverPending() {
        try {
            // A worker that crashed on attempt #10 cannot be reclaimed; mark it FAILED.
            int exhausted = mapper.expireExhausted();
            if (exhausted > 0) log.warn("Marked {} expired notification jobs FAILED", exhausted);
            for (long id : mapper.pendingIds(50)) schedule(id);
        } catch (RuntimeException e) {
            log.error("Pending notification scan failed; next scan will retry", e);
        }
    }
}
