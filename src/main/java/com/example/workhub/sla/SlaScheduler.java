package com.example.workhub.sla;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlaScheduler {
    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);
    private final SlaMapper mapper;
    private final SlaEscalationWorker worker;
    public SlaScheduler(SlaMapper mapper, SlaEscalationWorker worker) {
        this.mapper = mapper; this.worker = worker;
    }
    @Scheduled(fixedDelayString="${workhub.sla.scan-ms:60000}",
            initialDelayString="${workhub.sla.scan-ms:60000}")
    public void scan() {
        try {
            for (int ticketId : mapper.overdue(100)) {
                try { worker.escalate(ticketId); }
                catch (RuntimeException error) { log.error("SLA escalate failed for Ticket {}", ticketId, error); }
            }
        } catch (RuntimeException error) { log.error("SLA scan failed", error); }
    }
}
